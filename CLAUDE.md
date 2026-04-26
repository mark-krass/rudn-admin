# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Стек

Spring Boot 3.5 / Java 21 (toolchain), Gradle (wrapper), PostgreSQL 16, Keycloak 26 (OIDC), MinIO, Liquibase, MapStruct 1.6, Lombok, Testcontainers. Сборка через `bootJar` (classifier `application`), обычный `jar` отключён.

## Команды

- Сборка без тестов: `./gradlew clean build -x test`
- Полная сборка с тестами: `./gradlew build`
- Запуск тестов: `./gradlew test`
- Запуск одного теста: `./gradlew test --tests 'ru.rudn.rudnadmin.rest.student.StudentControllerTest'` (можно с `*` и `#methodName`)
- Локальный запуск приложения: `./gradlew bootRun` (требует поднятых зависимостей из `docker-compose.yml`)
- Сборка docker-образа после `gradle build`: `bash docker-build.sh` (Linux) или `./docker-build.ps1` (Windows) — собирает `rudn-admin-image:latest` из `Dockerfile`, который копирует `build/libs/rudn-admin-1.0-SNAPSHOT-application.jar`.
- Перед `docker compose up` один раз создать сеть: `docker network create admin_net` (она объявлена `external: true`).
- Полный стек: `docker compose up -d` (поднимает rudn-db, student-db, keycloak-db, keycloak, minio, rudn-admin). Остановка: `docker compose down`.
- Отладка: контейнер `rudn-admin` пробрасывает JDWP на порт 5005 (`JAVA_TOOL_OPTIONS` в compose).

Тесты используют Testcontainers — нужен запущенный Docker daemon. Базовый класс `src/test/java/ru/rudn/rudnadmin/config/TestContainersBase.java` поднимает PostgreSQL/MinIO/Keycloak контейнеры (см. `TestContainers.java`) и чистит БД и bucket после каждого теста.

## Архитектура

### Доменные модули (`src/main/java/ru/rudn/rudnadmin/`)

Код организован по фичам — каждая фича в `rest/<feature>/` содержит свой `Controller`, `ServiceImpl`, `Mapper` (MapStruct) и DTO; общие entity/repository — в корневых пакетах `entity/` и `repository/`. Фичи: `student`, `group`, `direction`, `user` (учётки в Keycloak), `vpn`, `postgres`. Глобальные обработчики исключений и общие модели — в `rest/global/`. Кросс-фичевые сервисы — в `service/`: `vpn/` (оркестрация задач), `openvpn/` (запуск shell-скриптов), `minio/` (объектное хранилище), `postgres/` (провижининг student-БД).

### Две базы данных

Приложение работает с двумя независимыми Postgres-инстансами:

1. **Основная (`rudn-db`, схема `admin`)** — управляется Spring Data JPA. Все entity (`User`, `Student`, `Group`, `Direction`, `Vpn`, `VpnTask`) и `*Repository` относятся к ней. Конфиг: `config/DatabaseConfig.java` (создаёт схему до старта Hibernate через `BeanPostProcessor`, потому что `ddl-auto: validate`). Liquibase: `src/main/resources/db/changelog/db.changelog-master.yaml` → `changes/*.sql`.
2. **Студенческая (`student-db`)** — НЕ управляется JPA. Используется только сервисом `service/postgres/` через сырой `DriverManager.getConnection()` для провижининга ролей/схем (см. `rest/postgres/`: эндпоинты создают изолированные схему+роль для группы или одного студента). README отмечает, что эта функциональность временно живёт здесь и со временем должна переехать в отдельный микросервис.

Параметры обеих БД задаются через env (`DB_*` и `STUDENT_DB_*`), маппинг — в `application.yml`.

### Безопасность (Keycloak / OAuth2 Resource Server)

`config/SecurityConfig.java`. Сервис — stateless JWT resource server. Issuer и client-id берутся из `KEYCLOAK_ISSUER_URI` и `KEYCLOAK_CLIENT_ID` (realm в dev — `rudn`, client — `rudn-admin-api`). Кастомный `JwtAuthenticationConverter` достаёт роли из `resource_access[client-id].roles` и префиксует их `ROLE_`. Правила доступа: `/api/users/**` — `ADMIN` либо `KEYCLOAK_MANAGER`; остальные `/api/**` — `ADMIN` либо `MANAGER`; Swagger/OpenAPI открыт. Realm для dev лежит в `volumes/keycloak/realm/` и автоимпортится контейнером `keycloak`.

### Поток выдачи VPN

`service/vpn/` реализует асинхронный пошаговый workflow: `VpnTask` хранит состояние, `VpnTaskScheduler` выполняет шаги через `VpnTaskStepExecutor`, конкретные шаги — отдельные `VpnTaskFlow` (CreateOvpn, UploadMinio, RenewLink, DeleteOvpn, DeleteMinio). Шаг CreateOvpn вызывает `service/openvpn/OpenVpnService`, который шеллит наружу: `VPN_CREATE_SCRIPT` / `VPN_DELETE_SCRIPT` (пути из env, скрипты живут в `volumes/admin/scripts/`, монтируются в контейнер). Сгенерированные `.ovpn` пишутся в `VPN_OUTPUT_DIR` и затем загружаются в MinIO bucket `vpn`; пользователю отдаётся пресайнед-ссылка (по умолчанию срок жизни 7 дней). Эндпоинты — в `rest/vpn/` (`VpnController`, `VpnSchedulerController`).

### Конфигурация

Все настройки — через env-переменные, маппятся в `src/main/resources/application.yml`:

- `DB_HOST/PORT/NAME/USERNAME/PASSWORD/SCHEMA` — основная БД
- `STUDENT_DB_HOST/PORT/USERNAME/PASSWORD` — студенческая БД (для `service/postgres/`)
- `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_CLIENT_ID` — OIDC
- `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` (+ `minio.bucket.name`, по умолчанию `vpn`)
- `VPN_CREATE_SCRIPT`, `VPN_DELETE_SCRIPT`, `VPN_OUTPUT_DIR`

Дефолтные значения для локальной разработки заданы в `docker-compose.yml`.

### TODO в репозитории (из README)

- Логику управления student-БД (`rest/postgres/`, `service/postgres/`) планируется вынести в отдельный микросервис.
- Генерацию DTO планируется вынести в библиотеку через `openApiGenerator`.

# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Назначение

Core-сервис РУДН для учёта студентов, выдачи VPN-конфигов и выдачи учёток PostgreSQL (см. `Readme.md`). Spring Boot 3.5.4, Java 21, Gradle.

## Команды

Сборка и тесты (Gradle wrapper):

```bash
./gradlew clean build          # полный build, включая тесты
./gradlew clean build -x test  # сборка без тестов (как в README)
./gradlew bootJar              # собрать только исполняемый jar
./gradlew test                 # прогнать все тесты
./gradlew test --tests 'ru.rudn.rudnadmin.rest.user.UserControllerTest'          # один класс
./gradlew test --tests 'ru.rudn.rudnadmin.rest.user.UserControllerTest.methodName' # один метод
```

Артефакт после `bootJar`: `build/libs/rudn-admin-1.0-SNAPSHOT-application.jar` (classifier `application`; обычный `jar` отключён). Dockerfile ожидает именно этот файл.

Локальный запуск стека (из `Readme.md`):

```bash
./gradlew clean build -x test
bash docker-build.sh            # собирает образ rudn-admin-image:latest
docker network create admin_net # сеть external, обязательна
docker compose up -d
docker compose down
```

Сервис открывает порт `8080` (HTTP) и `5005` (JDWP, suspend=n — можно цеплять отладчик в любое время). Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Архитектура

Пакет верхнего уровня — `ru.rudn.rudnadmin`. Точка входа: `RudnAdminApplication` (`@SpringBootApplication` + `@EnableScheduling`).

**REST-слой** организован по фичам, а не по технологиям. Под `rest/` каждая фича (`user`, `student`, `group`, `direction`, `vpn`, `postgres`) содержит свой `Controller`, `service/` + `service/impl/`, `mapper/` (MapStruct) и `model/` (DTO). `rest/global/` — общие exception-handler'ы, утилиты, общие модели ответов. Не смешивай слои между фичами — контроллер одной фичи не должен напрямую тянуть сервисы другой через пакеты-имплементации.

**Доменные сервисы** лежат под `service/` и отделены от REST: `service/minio`, `service/openvpn`, `service/postgres`, `service/vpn`. Это инфраструктурные обёртки (MinIO client, shell-скрипты VPN, провижининг пользователей во внешней БД), которые REST-сервисы вызывают как зависимости.

**VPN pipeline — это конечный автомат, а не синхронный вызов.** `VpnTask` хранит состояние задачи, а `VpnTaskFlow` — интерфейс одного шага (см. javadoc в `service/vpn/VpnTaskFlow.java`):

- create: `CREATE_OVPN` → `UPLOAD_MINIO` → `RENEW_LINK` → `isActive=false`
- renew: `RENEW_LINK` → `isActive=false`
- delete: `DELETE_MINIO` → `DELETE_OVPN` → `isActive=false`

`VpnConfig` собирает все реализации `VpnTaskFlow` в `Map<TaskType, VpnTaskFlow>` — добавляешь новый шаг = создаёшь бин `VpnTaskFlow` с нужным `TaskType`, он подхватывается автоматически. `VpnTaskScheduler` крутится по cron `0 0 23 * * *` (23:00 ежедневно), берёт все `isActive=true` задачи и прогоняет каждую через `VpnTaskStepExecutor.executeNextStep` до конца или ошибки; ошибки сохраняются в `VpnTask.error`, а не роняют весь прогон. Создание/удаление `.ovpn` файлов — внешние shell-скрипты, пути передаются в env-переменных `VPN_CREATE_SCRIPT` / `VPN_DELETE_SCRIPT` (в docker-compose смонтированы из `volumes/admin/scripts/`).

**Две БД.** Главная (`admin` schema в БД `rudn`) — JPA/Hibernate (`ddl-auto: validate`) + Liquibase миграции в `src/main/resources/db/changelog/`. Схема `admin` создаётся автоматически через `DatabaseConfig.createSchemaBeanPostProcessor` до старта Liquibase. Отдельный `student-db` — **не** JPA-источник: `PostgresService` подключается к нему напрямую и создаёт БД/роли на лету для групп и студентов (`createForGroup`, `createForStudent`). Это тот самый временный «каскадный механизм», который `Readme.md` помечен как TODO для выноса в отдельный микросервис — не закладывайся на его текущее расположение надолго.

**Безопасность — Keycloak JWT resource server.** `SecurityConfig` stateless, все эндпоинты, кроме Swagger, требуют роль; `/api/users/**` — `ADMIN` или `KEYCLOAK_MANAGER`, остальные `/api/**` — `ADMIN` или `MANAGER`. Роли берутся из `resource_access.<client-id>.roles` в JWT (client-id — `security.keycloak.client-id`, env `KEYCLOAK_CLIENT_ID`) и префиксуются `ROLE_`. Скоупы (`SCOPE_*`) тоже сохраняются. Dev realm автоимпортится в Keycloak из `volumes/keycloak/realm/rudn-realm.json`.

**Файлы VPN в MinIO.** Bucket `vpn`, конфиг — `MinioConfig` + env `MINIO_URL` / `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY`. Локально MinIO доступен на `http://localhost:9000` (S3) и `http://localhost:9001` (консоль).

## Конфигурация и окружение

`application.yml` в main/resources и test/resources **полностью** параметризован через `${ENV_VAR}` без дефолтов — локальные запуски без docker-compose требуют экспорта всех переменных (`DB_*`, `STUDENT_DB_*`, `KEYCLOAK_*`, `MINIO_*`, `VPN_*`). Canonical набор и дефолтные dev-значения — в `docker-compose.yml`.

## Тесты

JUnit 5 + `spring-boot-starter-test` + `spring-security-test` + Testcontainers (PostgreSQL, MinIO). Базовые классы для интеграционных тестов — `src/test/java/ru/rudn/rudnadmin/config/TestContainers.java` и `TestContainersBase.java`. Структура теста повторяет структуру main (`rest/<feature>/...ControllerTest.java`, `...MapperTest.java`; репозиторные тесты в `repository/`; безопасность — `rest/security/KeycloakSecurityTest.java`). `test/resources/application.yml` переопределяет пути VPN-скриптов на локальные `.ps1` из `test/resources/scripts/` — тестовый flow не стреляет по настоящим хостам.

## Важные соглашения

- **Lombok + MapStruct** используются везде. Mapper'ы — Spring-бины; регистрируй `@Mapper(componentModel = "spring")` и инжекти через конструктор.
- **Liquibase — authoritative источник схемы.** `hibernate.ddl-auto=validate`: любое изменение сущности без соответствующего changeSet = падение на старте. Добавляй новый SQL в `db/changelog/changes/` и регистрируй в `db.changelog-master.yaml`.
- **Не трогай classifier `application` у bootJar** без синхронного обновления `Dockerfile` (он копирует файл с конкретным именем).
- `jar { enabled = false }` — обычный jar отключён осознанно, единственный артефакт это bootJar.

# rudn-admin
Spring Boot backend for the RUDN administration system. Manages users, student data, role-based access via Keycloak, and OpenVPN configuration lifecycle (scripts + MinIO storage).

> Общие для всей RUDN-экосистемы правила (стек, git workflow, Keycloak,
> CSRF/CORS) см. в `../CLAUDE.md` (зонтичный).

## Quick start (local)
1. Ensure the external Docker network exists:
   ```bash
   docker network create admin_net
   ```
2. Start the supporting stack:
   ```bash
   docker compose up -d rudn-db student-db keycloak-db keycloak minio
   ```
3. Wait for Keycloak to import the realm from `volumes/keycloak/realm/rudn-realm.json`
   (check `docker compose logs -f keycloak` until you see "imported").
4. Run the app locally from IDE or:
   ```bash
   ./gradlew bootRun
   ```
   Requires the env vars listed below (typically loaded from `.env` or IDE run config).
## Ports and endpoints (dev)
| Service           | Port  | URL / Notes                                    |
| ----------------- | ----- | ---------------------------------------------- |
| rudn-admin (app)  | 8080  | http://localhost:8080                          |
| Swagger UI        | 8080  | http://localhost:8080/swagger-ui.html          |
| OpenAPI JSON      | 8080  | http://localhost:8080/v3/api-docs              |
| JVM debug         | 5005  | attach your IDE debugger here                  |
| rudn DB (main)    | 5432  | postgres, db=`rudn`, schema=`admin`            |
| student DB        | 5433  | postgres, db=`postgres`                        |
| keycloak DB       | 5434  | postgres, internal to Keycloak                 |
| Keycloak          | 8081  | http://localhost:8081, realm `rudn`            |
| MinIO API         | 9000  | http://localhost:9000                          |
| MinIO Console     | 9001  | http://localhost:9001 (minioadmin / minioadmin)|
## Required environment variables
See `application.yml` for the full list. Key ones:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SCHEMA` — main Postgres
- `STUDENT_DB_HOST`, `STUDENT_DB_PORT`, `STUDENT_DB_USERNAME`, `STUDENT_DB_PASSWORD` — student Postgres
- `KEYCLOAK_ISSUER_URI` (e.g. `http://localhost:8081/realms/rudn`)
- `KEYCLOAK_CLIENT_ID` (currently `rudn-admin-api`)
- `MINIO_URL`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`
- `VPN_CREATE_SCRIPT`, `VPN_DELETE_SCRIPT`, `VPN_OUTPUT_DIR` — OpenVPN generator scripts
## Entry points
- `ru.rudn.rudnadmin` — root package
- `config/SecurityConfig.java` — JWT extraction, role mapping, endpoint rules
- `rest/<resource>/` — REST controllers per feature
- `service/vpn/` — OpenVPN lifecycle (scheduler + script execution)
- `service/minio/` — MinIO client wrapper for `vpn` bucket
- `src/main/resources/db/changelog/` — Liquibase (master in YAML, changesets in SQL)
## Project-specific facts
- **Две Postgres-БД**: `rudn` (основная, owned by this app, schema `admin`, таблица `rudn_user` и др.) и `student` (внешний read-mostly источник).
- **MinIO bucket `vpn`** хранит `.ovpn` файлы, генерируемые скриптами из `VPN_CREATE_SCRIPT` / `VPN_DELETE_SCRIPT`.
- **Role matrix**: `/api/**` → роли `ADMIN` или `MANAGER` (см. `SecurityConfig`).
- **Liquibase default schema** = `admin` (через `DB_SCHEMA`, Liquibase сконфигурирован с `default-schema`).
## Known gotchas
- `docker-compose.yml` requires an **external** network `admin_net`. Create it once: `docker network create admin_net`.
- Keycloak's `rudn-realm.json` is auto-imported only on first start (thanks to `--import-realm`). To re-import after edits, remove the `keycloak-data` volume.
- The `student.datasource.url` in `application.yml` does NOT include a database name after the port — it relies on the default DB or the env-configured one. If JDBC errors appear at startup, check this first.
- Keycloak bootstrap-креды в `docker-compose.yml` — placeholder для локалки, не должны попасть в прод-compose/values.
- CI/CD is not set up yet. All tests must pass locally before PR (`./gradlew test`).

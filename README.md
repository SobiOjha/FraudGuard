# Sentinel

Sentinel is an explainable, rule-based fraud detection and protection service
for server-to-server integrations. It evaluates transaction context and
behavioral history, returns a risk score from 0 to 100, and produces an
`APPROVE`, `FLAG`, or `BLOCK` decision.

## Architecture

```text
Browser -> React dashboard -> Node.js BFF -> Spring Boot Sentinel API -> PostgreSQL/Neon
```

The Node BFF serves the built dashboard and is the browser security boundary.
The Sentinel `X-API-Key` is stored only in the BFF environment and is added to
server-to-server requests. It is never included in React source, `VITE_*`
variables, browser storage, or browser responses.

## Fraud behavior

The backend uses rule-based behavioral scoring, not machine learning. It
considers amount, recipient and device history, location, recent frequency,
and repeated recipient activity. Scores are classified as:

- `0-29`: `SAFE`
- `30-59`: `SUSPICIOUS`
- `60-79`: `HIGH`
- `80-100`: `CRITICAL`

Protection modes are `NORMAL`, `CAUTION`, and `PROTECTED`.

## API security

- `POST /api/auth/register` and `POST /api/auth/login` are public user/admin-foundation endpoints.
- `/api/integrations/**` requires an `ADMIN` user through HTTP Basic Authentication; state-changing requests require CSRF protection.
- `/api/transactions/**` requires `X-API-Key` and is scoped to the authenticated Integration.
- Unknown backend routes are denied.
- The BFF exposes only the dashboard transaction routes and does not proxy authentication or integration-management endpoints.

Every new transaction is owned by the authenticated Integration. The caller's
`userId` remains the external system's customer/account identifier. Behavioral
history, reads, updates, and dashboard statistics are isolated by Integration
and do not accept a client-supplied `integrationId`.

## Local development

Backend:

```powershell
cd fraudguard-backend
$env:DB_PASSWORD="<local-postgres-password>"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Frontend/BFF:

```powershell
cd fraudguard-frontend
$env:SENTINEL_BACKEND_URL="http://localhost:8080"
$env:SENTINEL_API_KEY="<server-only-integration-key>"
$env:BFF_USERNAME="<dashboard-username>"
$env:BFF_PASSWORD="<dashboard-password>"
npm ci
npm run build
npm start
```

The BFF requires `SENTINEL_BACKEND_URL`, `SENTINEL_API_KEY`, `BFF_USERNAME`,
and `BFF_PASSWORD` as server-only variables. Do not use `VITE_` prefixes for
them. Vite development can proxy `/bff` to the BFF with `BFF_URL`.

Development and production use `spring.jpa.hibernate.ddl-auto=validate`.
Flyway manages schema changes; do not use Hibernate `update`, `create`, or
`create-drop` against a shared database.

## Validation

Backend tests:

```powershell
cd fraudguard-backend
mvn clean test
```

Frontend checks:

```powershell
cd fraudguard-frontend
npm run test:bff
npm run lint
npm run build
```

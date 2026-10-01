# Sentinel API-key authentication

Sentinel authenticates external server integrations with an `X-API-Key` header.
Each integration has a name, an active flag, a creation timestamp, and a SHA-256
hash of its API key. The raw key is generated with `SecureRandom`, returned only
from the administrative creation response, and is never persisted or logged.

Integration management requires HTTP Basic credentials for an existing
`ADMIN` user:

First obtain a CSRF token using the admin credentials:

```http
GET /api/integrations/csrf-token
Authorization: Basic <admin-credentials>
```

Keep the returned `XSRF-TOKEN` cookie and token value. Send both the cookie
and `X-XSRF-TOKEN` header on state-changing integration-management requests.

```http
POST /api/integrations
Authorization: Basic <admin-credentials>
X-XSRF-TOKEN: <csrf-token>
Content-Type: application/json

{"name":"Demo Payment System"}
```

Store the returned `apiKey` securely. External systems call protected Sentinel
business APIs with:

```http
X-API-Key: <integration-api-key>
```

An administrator can revoke a key with
`PATCH /api/integrations/{id}/status` and `{"active":false}`. Revoked or
unknown keys receive HTTP 401. The `V1__create_integrations.sql` Flyway
migration creates the required production table while `ddl-auto=validate`
remains unchanged. The legacy `/api/auth/register` and `/api/auth/login`
routes are disabled because Sentinel uses API keys for external integrations;
the database-backed `User`/`ADMIN` foundation remains for administration.

## Schema management

Development and production both use `spring.jpa.hibernate.ddl-auto=validate`.
Do not use Hibernate `update`, `create`, or `create-drop` against a shared
database. For an existing development database, take a backup, ensure the
development `DB_PASSWORD` is available, and start the application with the
`dev` profile. Flyway's configured baseline-at-version-0 behavior records the
existing non-empty schema as baseline `0`, then applies `V1__create_integrations.sql`.
Verify `flyway_schema_history` and the `integrations` table afterward. Do not
drop or reset the database. A fresh database also needs migrations for the
pre-existing Sentinel tables before `ddl-auto=validate` can start successfully;
that is separate from the integrations migration in this phase.

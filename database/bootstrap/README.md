# FraudGuard fresh PostgreSQL installation

This procedure is opt-in and is never run by normal application startup. It
creates the canonical current schema, then records Flyway baseline version 3.
It does not run V1, V2, or V3 and does not insert application data.

The canonical schema is a fresh-install design based on the current entity
mappings and known local PostgreSQL shape. It is not a reconstruction of the
unavailable historical pre-V2 DDL.

## Existing databases

Do not run the bootstrap against an existing database. Databases with recorded
V1-V3 history must retain that history and those checksums. V3 remains
unchanged and must be included in source control byte-for-byte.

Normal startup has spring.flyway.baseline-on-migrate=false and
spring.jpa.hibernate.ddl-auto=validate. Existing V1-V3 databases retain their
history; an explicitly baselined fresh database starts at version 3. Future
V4 and later migrations therefore apply to both database classes.

## Disposable local test

Create and identify a uniquely named database outside this repository using an
operator-controlled PostgreSQL administrative session. The name must match
fraudguard_bootstrap_test_<unique_suffix>. Never use fraudguard.

The system identifier must be obtained from the intended local PostgreSQL
instance through an authorized read-only check and must never be guessed. If
it cannot be obtained or the connection cannot prove it matches, the bootstrap
aborts before DDL. The target URL host and the expected host may be aliases,
but the connected address must resolve through both and match the expected
port and system identifier.

Run the following from the repository root, replacing placeholders only with
the independently verified target values:

    .\database\bootstrap\bootstrap-fresh-postgres.ps1 -TargetDatabaseUrl 'jdbc:postgresql://127.0.0.1:5432/fraudguard_bootstrap_test_<unique_suffix>' -TargetDatabaseUser '<local-postgres-user>' -TargetDatabaseName 'fraudguard_bootstrap_test_<unique_suffix>' -ExpectedServerHost '127.0.0.1' -ExpectedServerPort 5432 -ExpectedServerIdentity '<trusted-system-identifier>' -TargetKind DisposableLocalTest -AllowApprovedFreshInstall

The script rejects protected names, checks the connected database and server
identity, refuses user-created catalog objects, uses one PostgreSQL transaction
for schema creation, and verifies that the only Flyway history row is the
successful version-3 baseline.

Schema creation and Flyway baseline are separate operations, so the procedure
does not claim cross-process atomicity. If baseline or verification fails, a
non-secret recovery manifest remains and reruns are refused until the target is
explicitly inspected and recovered. No automatic drop or reset is attempted.
The manifest records the target identity and expected created objects, not
credentials.

After successful validation, verify the exact disposable database name again
and remove only that disposable database through the operator-controlled local
PostgreSQL administrative session. Do not use a wildcard or a
variable-derived cleanup target.

The disposable test must verify empty-schema refusal, schema/catalog shape, the
single successful baseline-3 row, Flyway validation with no pending migrations,
Hibernate validation, and future V4 application in a temporary test-only
migration location. The real migration directory and fraudguard database must
not be used for the V4 check.

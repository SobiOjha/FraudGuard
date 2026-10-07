[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $TargetDatabaseUrl,
    [Parameter(Mandatory = $true)]
    [string] $TargetDatabaseUser,
    [Parameter(Mandatory = $true)]
    [string] $TargetDatabaseName,
    [Parameter(Mandatory = $true)]
    [ValidateSet('DisposableLocalTest', 'ApprovedFreshInstall')]
    [string] $TargetKind,
    [Parameter(Mandatory = $true)]
    [string] $ExpectedServerHost,
    [Parameter(Mandatory = $true)]
    [ValidateRange(1, 65535)]
    [int] $ExpectedServerPort,
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[0-9]+$')]
    [string] $ExpectedServerIdentity,
    [switch] $AllowApprovedFreshInstall,
    [string] $PsqlPath = 'C:\Program Files\PostgreSQL\18\bin\psql.exe',
    [string] $MavenPath = 'mvn.cmd'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$protectedDatabaseNames = @('fraudguard', 'postgres', 'template0', 'template1')

function Resolve-AddressTexts {
    param([Parameter(Mandatory = $true)][string] $HostName)
    try {
        return @(
            [System.Net.Dns]::GetHostAddresses($HostName) |
                ForEach-Object { $_.ToString() } |
                Sort-Object -Unique
        )
    } catch {
        throw 'Could not resolve the expected database host.'
    }
}

function Test-LoopbackAddress {
    param([Parameter(Mandatory = $true)][string] $Address)
    try {
        return [System.Net.IPAddress]::IsLoopback([System.Net.IPAddress]::Parse($Address))
    } catch {
        return $false
    }
}

if (-not (Test-Path -LiteralPath $PsqlPath -PathType Leaf)) {
    throw 'PostgreSQL 18 psql was not found at the specified path.'
}
if ($TargetDatabaseUrl -notmatch '^jdbc:postgresql://') {
    throw 'TargetDatabaseUrl must be a PostgreSQL JDBC URL.'
}
if ($TargetDatabaseUrl -match '(?i)(^|[?&])(password|passfile|sslkey|sslcert|sslrootcert)=') {
    throw 'TargetDatabaseUrl must not contain credential or private-key parameters.'
}
if ($TargetDatabaseUrl -match '(?i)(^|[?&])sslmode=disable(&|$)') {
    throw 'TargetDatabaseUrl must not disable PostgreSQL TLS.'
}

$databaseUri = [Uri]($TargetDatabaseUrl -replace '^jdbc:', '')
if (-not [string]::IsNullOrWhiteSpace($databaseUri.UserInfo)) {
    throw 'TargetDatabaseUrl must not contain URL user information.'
}
if ($databaseUri.Port -le 0) {
    throw 'TargetDatabaseUrl must specify an explicit PostgreSQL port.'
}

$normalizedDatabaseName = $TargetDatabaseName.Trim()
if ($normalizedDatabaseName -ne $TargetDatabaseName -or $normalizedDatabaseName -notmatch '^[a-z0-9][a-z0-9_-]{0,62}$') {
    throw 'TargetDatabaseName must be an unambiguous lowercase PostgreSQL database name.'
}
$parsedDatabaseName = [Uri]::UnescapeDataString($databaseUri.AbsolutePath.TrimStart('/'))
if ($parsedDatabaseName -ne $normalizedDatabaseName) {
    throw 'The database name in TargetDatabaseUrl does not exactly match TargetDatabaseName.'
}
if ($protectedDatabaseNames -contains $normalizedDatabaseName.ToLowerInvariant()) {
    throw "The protected database name '$normalizedDatabaseName' cannot be used for bootstrap."
}
if (-not $AllowApprovedFreshInstall) {
    throw 'Explicit approval is required. Re-run with -AllowApprovedFreshInstall after independently verifying the target.'
}
if ($TargetKind -eq 'DisposableLocalTest' -and $normalizedDatabaseName -notmatch '^fraudguard_bootstrap_test_[a-z0-9_-]+$') {
    throw 'DisposableLocalTest database names must use the fraudguard_bootstrap_test_ prefix.'
}
if ($databaseUri.Port -ne $ExpectedServerPort) {
    throw 'The JDBC URL port does not match the explicitly expected server port.'
}

$targetAddresses = @(Resolve-AddressTexts $databaseUri.DnsSafeHost)
$expectedAddresses = @(Resolve-AddressTexts $ExpectedServerHost)
if ($targetAddresses.Count -eq 0 -or $expectedAddresses.Count -eq 0) {
    throw 'The target or expected server host did not resolve.'
}

$manifestPath = Join-Path ([System.IO.Path]::GetTempPath()) "fraudguard-bootstrap-$normalizedDatabaseName.json"
if (Test-Path -LiteralPath $manifestPath) {
    throw 'A recovery manifest already exists for this database name. Inspect and recover it before rerunning.'
}

$expectedCreatedObjects = @(
    'public.users', 'public.users_id_seq', 'public.users_pkey',
    'public.users_username_key', 'public.users_role_check',
    'public.integrations', 'public.integrations_id_seq',
    'public.integrations_pkey', 'public.integrations_api_key_hash_key',
    'public.transactions', 'public.transactions_id_seq',
    'public.transactions_pkey', 'public.idx_transactions_integration_id',
    'public.fk_transactions_integration', 'public.transaction_reasons',
    'public.fk_transaction_reasons_transaction',
    'public.flyway_schema_history', 'public.flyway_schema_history_pk'
)

$securePassword = $null
$passwordPtr = [IntPtr]::Zero

try {
    $securePassword = Read-Host 'PostgreSQL password for the explicitly verified target' -AsSecureString
    $passwordPtr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    $env:PGPASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPtr)

    $psqlArgs = @('-h', $databaseUri.DnsSafeHost, '-p', [string]$databaseUri.Port, '-U', $TargetDatabaseUser, '-d', $normalizedDatabaseName, '-w', '-X', '-v', 'ON_ERROR_STOP=1', '-q', '-tA', '-F', '|')
    $identitySql = @'
SELECT current_database()
       || '|' || COALESCE(host(inet_server_addr()), '')
       || '|' || inet_server_port()::text
       || '|' || (SELECT system_identifier::text FROM pg_control_system());
'@
    $identityOutput = @(& $PsqlPath @psqlArgs -c $identitySql 2>$null)
    if ($LASTEXITCODE -ne 0 -or $identityOutput.Count -ne 1) {
        throw 'The connected server identity could not be established; no DDL was attempted.'
    }
    $identityParts = @($identityOutput[0].Trim() -split '\|', 4)
    if ($identityParts.Count -ne 4) {
        throw 'The connected server identity response was ambiguous; no DDL was attempted.'
    }
    $actualDatabaseName = $identityParts[0]
    $actualServerAddress = $identityParts[1]
    $actualServerPort = $identityParts[2]
    $actualServerIdentity = $identityParts[3]
    if ($actualDatabaseName -cne $normalizedDatabaseName) {
        throw 'The connected database name did not match the approved target; no DDL was attempted.'
    }
    if ($actualServerPort -ne [string]$ExpectedServerPort -or $actualServerIdentity -ne $ExpectedServerIdentity) {
        throw 'The connected server did not match the approved port and server identity; no DDL was attempted.'
    }
    if ($targetAddresses -notcontains $actualServerAddress -or $expectedAddresses -notcontains $actualServerAddress) {
        throw 'The connected server address did not match both the target URL and expected host; no DDL was attempted.'
    }
    if ($TargetKind -eq 'DisposableLocalTest' -and (-not (Test-LoopbackAddress $actualServerAddress) -or @($targetAddresses | Where-Object { -not (Test-LoopbackAddress $_) }).Count -gt 0 -or @($expectedAddresses | Where-Object { -not (Test-LoopbackAddress $_) }).Count -gt 0)) {
        throw 'DisposableLocalTest requires a loopback PostgreSQL server; no DDL was attempted.'
    }
    if ($TargetKind -eq 'ApprovedFreshInstall' -and (-not (Test-LoopbackAddress $actualServerAddress)) -and $TargetDatabaseUrl -notmatch '(?i)(^|[?&])sslmode=verify-full(&|$)') {
        throw 'Non-local approved targets require sslmode=verify-full; no DDL was attempted.'
    }

    $emptyCatalogSql = @'
WITH object_schemas AS (
    SELECT oid, nspname
    FROM pg_namespace
    WHERE nspname = 'public'
       OR (
           nspname NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
           AND nspname !~ '^pg_temp_[0-9]+$'
           AND nspname !~ '^pg_toast_temp_[0-9]+$'
       )
)
SELECT 'schema|' || nspname FROM object_schemas WHERE nspname <> 'public'
UNION ALL SELECT 'relation|' || n.nspname || '.' || c.relname FROM pg_class c JOIN object_schemas n ON n.oid = c.relnamespace
UNION ALL SELECT 'function|' || n.nspname || '.' || p.proname || '/' || p.oid::text FROM pg_proc p JOIN object_schemas n ON n.oid = p.pronamespace
UNION ALL SELECT 'type|' || n.nspname || '.' || t.typname FROM pg_type t JOIN object_schemas n ON n.oid = t.typnamespace
UNION ALL SELECT 'extension|' || e.extname FROM pg_extension e WHERE e.extname <> 'plpgsql'
UNION ALL SELECT 'language|' || l.lanname FROM pg_language l WHERE l.lanname NOT IN ('internal', 'c', 'sql', 'plpgsql')
UNION ALL SELECT 'foreign_server|' || s.srvname FROM pg_foreign_server s
UNION ALL SELECT 'foreign_data_wrapper|' || f.fdwname FROM pg_foreign_data_wrapper f
UNION ALL SELECT 'user_mapping|' || u.umuser::text || '|' || u.umserver::text FROM pg_user_mapping u
UNION ALL SELECT 'event_trigger|' || e.evtname FROM pg_event_trigger e
UNION ALL SELECT 'publication|' || p.pubname FROM pg_publication p
UNION ALL SELECT 'subscription|' || s.subname FROM pg_subscription s
UNION ALL SELECT 'replication_slot|' || r.slot_name FROM pg_replication_slots r
UNION ALL SELECT 'tablespace|' || t.spcname FROM pg_tablespace t WHERE t.spcname NOT IN ('pg_default', 'pg_global')
UNION ALL SELECT 'default_acl|' || d.oid::text FROM pg_default_acl d
UNION ALL SELECT 'collation|' || n.nspname || '.' || c.collname FROM pg_collation c JOIN object_schemas n ON n.oid = c.collnamespace WHERE n.nspname = 'public'
UNION ALL SELECT 'conversion|' || n.nspname || '.' || c.conname FROM pg_conversion c JOIN object_schemas n ON n.oid = c.connamespace
UNION ALL SELECT 'operator|' || n.nspname || '.' || o.oprname FROM pg_operator o JOIN object_schemas n ON n.oid = o.oprnamespace
UNION ALL SELECT 'opclass|' || n.nspname || '.' || o.opcname FROM pg_opclass o JOIN object_schemas n ON n.oid = o.opcnamespace
UNION ALL SELECT 'opfamily|' || n.nspname || '.' || o.opfname FROM pg_opfamily o JOIN object_schemas n ON n.oid = o.opfnamespace
UNION ALL SELECT 'text_search_config|' || n.nspname || '.' || t.cfgname FROM pg_ts_config t JOIN object_schemas n ON n.oid = t.cfgnamespace
UNION ALL SELECT 'text_search_dict|' || n.nspname || '.' || t.dictname FROM pg_ts_dict t JOIN object_schemas n ON n.oid = t.dictnamespace
UNION ALL SELECT 'text_search_parser|' || n.nspname || '.' || t.prsname FROM pg_ts_parser t JOIN object_schemas n ON n.oid = t.prsnamespace
UNION ALL SELECT 'text_search_template|' || n.nspname || '.' || t.tmplname FROM pg_ts_template t JOIN object_schemas n ON n.oid = t.tmplnamespace
ORDER BY 1;
'@
    $catalogObjects = @(& $PsqlPath @psqlArgs -c $emptyCatalogSql 2>$null | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    if ($LASTEXITCODE -ne 0) {
        throw 'The comprehensive emptiness check failed; no DDL was attempted.'
    }
    if ($catalogObjects.Count -ne 0) {
        throw 'The target contains existing database objects; no DDL was attempted.'
    }

    $manifest = [ordered]@{
        manifestVersion = 1
        databaseName = $normalizedDatabaseName
        expectedServerHost = $ExpectedServerHost
        expectedServerPort = $ExpectedServerPort
        expectedServerIdentity = $ExpectedServerIdentity
        expectedCreatedObjects = $expectedCreatedObjects
        status = 'preflight-verified'
    }
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8

    $schemaPath = Join-Path $PSScriptRoot 'current-schema.sql'
    $schemaArgs = $psqlArgs + @('-1', '-f', $schemaPath)
    $null = & $PsqlPath @schemaArgs 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw 'Canonical schema creation failed and was rolled back by PostgreSQL.'
    }

    $schemaVerificationSql = @'
SELECT
    (
        SELECT COUNT(*)
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
          AND c.relname = ANY (ARRAY[
              'users', 'users_id_seq', 'users_pkey', 'users_username_key',
              'integrations', 'integrations_id_seq', 'integrations_pkey',
              'integrations_api_key_hash_key', 'transactions',
              'transactions_id_seq', 'transactions_pkey',
              'idx_transactions_integration_id', 'transaction_reasons'
          ]::name[])
    )::text
    || '|' ||
    (
        SELECT COUNT(*)
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'public'
    )::text
    || '|' ||
    (
        SELECT COUNT(*)
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'public'
          AND c.conname = ANY (ARRAY[
              'users_pkey', 'users_username_key', 'users_role_check',
              'integrations_pkey', 'integrations_api_key_hash_key',
              'transactions_pkey', 'fk_transactions_integration',
              'fk_transaction_reasons_transaction'
          ]::name[])
    )::text
    || '|' ||
    (
        SELECT COUNT(*)
        FROM pg_constraint c
        JOIN pg_class r ON r.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = r.relnamespace
        WHERE n.nspname = 'public'
    )::text;
'@
    $schemaVerification = @(& $PsqlPath @psqlArgs -c $schemaVerificationSql 2>$null)
    if ($LASTEXITCODE -ne 0 -or $schemaVerification.Count -ne 1 -or $schemaVerification[0].Trim() -ne '13|13|8|26') {
        throw 'The created schema did not match the recorded canonical object set. Recovery is required.'
    }

    $manifest.verifiedCreatedObjects = $expectedCreatedObjects
    $manifest.status = 'schema-created-awaiting-baseline'
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8

    $pomPath = Join-Path $PSScriptRoot '..\..\fraudguard-backend\pom.xml'
    [xml]$pom = Get-Content -LiteralPath $pomPath
    $flywayVersion = [string]$pom.project.properties.'flyway.version'
    if ($flywayVersion -notmatch '^[0-9]+\.[0-9]+\.[0-9]+([.-][A-Za-z0-9.-]+)?$') {
        throw 'The project does not expose one explicit Flyway version for the bootstrap plugin.'
    }
    $env:FLYWAY_URL = $TargetDatabaseUrl
    $env:FLYWAY_USER = $TargetDatabaseUser
    $env:FLYWAY_PASSWORD = $env:PGPASSWORD
    $env:FLYWAY_SCHEMAS = 'public'
    $env:FLYWAY_DEFAULT_SCHEMA = 'public'
    $env:FLYWAY_BASELINE_VERSION = '3'
    $env:FLYWAY_BASELINE_DESCRIPTION = 'FraudGuard canonical fresh schema'
    $env:FLYWAY_BASELINE_ON_MIGRATE = 'false'
    $env:FLYWAY_CREATE_SCHEMAS = 'false'
    $flywayGoal = "org.flywaydb:flyway-maven-plugin:${flywayVersion}:baseline"
    $null = & $MavenPath '-q' '-f' $pomPath $flywayGoal 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw 'Flyway baseline failed. Recovery is required; the manifest prevents an unsafe rerun.'
    }

    $historySql = @'
SELECT
    COUNT(*) FILTER (WHERE type = 'BASELINE' AND version = '3' AND description = 'FraudGuard canonical fresh schema' AND success = TRUE)::text
    || '|' || COUNT(*) FILTER (WHERE version IN ('1', '2', '3'))::text
    || '|' || COUNT(*)::text
FROM public.flyway_schema_history;
'@
    $history = @(& $PsqlPath @psqlArgs -c $historySql 2>$null)
    if ($LASTEXITCODE -ne 0 -or $history.Count -ne 1 -or $history[0].Trim() -ne '1|1|1') {
        throw 'Flyway baseline verification failed. Recovery is required; the manifest prevents an unsafe rerun.'
    }

    $manifest.status = 'complete'
    $manifest | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $manifestPath -Encoding utf8
    Remove-Item -LiteralPath $manifestPath -ErrorAction Stop
    Write-Output "Canonical schema and Flyway baseline 3 created in verified database '$normalizedDatabaseName'."
    Write-Output 'Automatic baselining was disabled. Start the application separately with Hibernate ddl-auto=validate.'
}
catch {
    if (Test-Path -LiteralPath $manifestPath) {
        $message = $_.Exception.Message
        throw "$message Recovery is required; inspect '$manifestPath' before any rerun."
    }
    throw
}
finally {
    Remove-Item Env:FLYWAY_URL -ErrorAction SilentlyContinue
    Remove-Item Env:FLYWAY_USER -ErrorAction SilentlyContinue
    Remove-Item Env:FLYWAY_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue
    if ($passwordPtr -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPtr)
    }
    if ($null -ne $securePassword) {
        $securePassword.Dispose()
    }
}

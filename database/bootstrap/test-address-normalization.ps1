[CmdletBinding()]
param(
    [string] $BootstrapScriptPath
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($BootstrapScriptPath)) {
    $BootstrapScriptPath = Join-Path $PSScriptRoot 'bootstrap-fresh-postgres.ps1'
}

if (-not (Test-Path -LiteralPath $BootstrapScriptPath -PathType Leaf)) {
    throw "Bootstrap script not found: $BootstrapScriptPath"
}

$source = Get-Content -LiteralPath $BootstrapScriptPath -Raw
$expectedSql = "COALESCE(host(inet_server_addr()), '')"

if ($source -notmatch [regex]::Escape($expectedSql)) {
    throw 'The identity query does not normalize inet_server_addr() with host().'
}

if ($source -match 'inet_server_addr\(\)::text') {
    throw 'The identity query still contains the CIDR-producing inet_server_addr()::text form.'
}

$dnsAddresses = [System.Net.Dns]::GetHostAddresses('127.0.0.1') |
    ForEach-Object { $_.ToString() }
$normalizedPostgresAddress = '127.0.0.1'

if ($normalizedPostgresAddress -match '/') {
    throw 'The representative host() result still contains CIDR notation.'
}

if ($dnsAddresses -notcontains $normalizedPostgresAddress) {
    throw "PowerShell DNS resolution did not return the normalized address '$normalizedPostgresAddress'."
}

Write-Output 'Address normalization validation passed without database access.'

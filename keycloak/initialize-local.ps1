# Generates ignored local secrets/import files; never changes existing realm users.
$ErrorActionPreference = 'Stop'
$backendRoot = Split-Path $PSScriptRoot -Parent
$envPath = Join-Path $backendRoot '.env.keycloak.local'
$importDir = Join-Path $PSScriptRoot 'local-import'
if (Test-Path -LiteralPath $envPath) { throw 'Local Keycloak configuration already exists. Reuse it; do not rotate credentials by rerunning initialization.' }
$bootstrapPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(24))
$appPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(16))
Set-Content -LiteralPath $envPath -Value "KEYCLOAK_BOOTSTRAP_PASSWORD=$bootstrapPassword" -Encoding utf8
New-Item -ItemType Directory -Force -Path $importDir | Out-Null
$realm = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'realm-template.json') -Raw | ConvertFrom-Json
$realm.users[0].credentials = @(@{type='password';value=$appPassword;temporary=$false})
$realm | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath (Join-Path $importDir 'eshop-realm.json') -Encoding utf8
@("E-Shop login: eshop-admin", "E-Shop password: $appPassword", "Keycloak console login: keycloak-admin", "Keycloak console password: $bootstrapPassword") | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'local-credentials.txt') -Encoding utf8
Write-Output 'Created ignored local Keycloak configuration and credentials.'

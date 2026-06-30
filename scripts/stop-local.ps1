$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$withData = $args -contains "-WithData"

if ($withData) {
    docker compose down -v
    Write-Host "Stopped local deployment and removed MySQL/MinIO volumes."
} else {
    docker compose down
    Write-Host "Stopped local deployment. Data volumes are preserved."
}

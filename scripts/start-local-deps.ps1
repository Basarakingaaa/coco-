$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "Docker Desktop is required. Install and start Docker Desktop first."
}

docker compose version | Out-Null

if (-not (Test-Path ".env")) {
    Copy-Item ".env.example" ".env"
    Write-Host "Created .env from .env.example. Review it if you need custom ports or credentials."
}

docker compose up -d mysql redis minio

Write-Host ""
Write-Host "Local dependencies are running."
Write-Host "MySQL:         localhost:3306"
Write-Host "Redis:         localhost:6379"
Write-Host "MinIO API:     http://localhost:9000"
Write-Host "MinIO Console: http://localhost:9001"

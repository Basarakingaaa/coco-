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

docker compose up -d --build

Write-Host ""
Write-Host "Local deployment is starting."
Write-Host "Frontend:      http://localhost:5173"
Write-Host "Backend API:   http://localhost:8080/api/products"
Write-Host "MinIO Console: http://localhost:9001"
Write-Host "Admin login:   admin / admin123"

#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Start applications with RabbitMQ messaging
.DESCRIPTION
    This script starts applications using RabbitMQ for messaging.
    - Native API: Uses Azure Service Bus directly (requirement: can only use Azure Service Bus)
    - Reversible API: Uses DAPR with RabbitMQ components for local development
.PARAMETER Clean
    Clean up existing containers before starting
.PARAMETER Build
    Force rebuild of containers before starting
.EXAMPLE
    .\start-rabbitmq.ps1
    .\start-rabbitmq.ps1 -Clean
    .\start-rabbitmq.ps1 -Build
#>

param(
    [switch]$Clean,
    [switch]$Build
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "🐇 RabbitMQ start script initializing..." -ForegroundColor DarkCyan

Write-Host "🚀 Starting applications with RabbitMQ messaging..." -ForegroundColor Green
Write-Host "   📨 api → DAPR → RabbitMQ" -ForegroundColor Yellow
Write-Host "   👁️ observer → DAPR → RabbitMQ" -ForegroundColor Yellow
Write-Host "📁 Working directory: $(Get-Location)" -ForegroundColor Cyan

# Check if Docker is running
try {
    docker version | Out-Null
    Write-Host "✅ Docker is running" -ForegroundColor Green
} catch {
    Write-Error "❌ Docker is not running or not installed. Please start Docker Desktop."
    exit 1
}

# Stop any existing environment before starting
Write-Host "🛑 Ensuring any existing environment is stopped..." -ForegroundColor Yellow
try {
    & "$PSScriptRoot\stop-apps.ps1"
} catch {
    Write-Host "⚠️ Could not stop existing environment (continuing): $($_.Exception.Message)" -ForegroundColor DarkYellow
}

# Navigate to docker directory
Set-Location "docker"

# No .env needed; compose files contain concrete values
Write-Host "🌍 Environment: RabbitMQ" -ForegroundColor Cyan
Write-Host "🔧 DAPR Components: components-rabbitmq" -ForegroundColor Cyan

# Clean up if requested
if ($Clean) {
    Write-Host "🧹 Cleaning up existing containers and networks..." -ForegroundColor Yellow
    docker-compose -f docker-compose-rabbitmq.yml down --remove-orphans
    docker system prune -f --volumes
}

# Build if requested or if images don't exist
if ($Build) {
    Write-Host "🔨 Building containers..." -ForegroundColor Yellow
    & .\build-containers.ps1
    if ($LASTEXITCODE -ne 0) {
        Write-Error "❌ Failed to build containers"
        exit 1
    }
}

# Start the services
Write-Host "🏁 Starting services (RabbitMQ profile)..." -ForegroundColor Yellow

try {
    # Start infrastructure services
    Write-Host "�️ Starting infrastructure services..." -ForegroundColor Cyan
    docker-compose -f docker-compose-rabbitmq.yml up -d postgres aspire-dashboard
    if ($LASTEXITCODE -ne 0) { throw "Failed to start infrastructure services" }

    Write-Host "🐰 Starting RabbitMQ..." -ForegroundColor Cyan
    docker-compose -f docker-compose-rabbitmq.yml --profile rabbitmq up -d rabbitmq
    if ($LASTEXITCODE -ne 0) { throw "Failed to start RabbitMQ" }

    Write-Host "⏳ Waiting for infrastructure to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10

    Write-Host "🔧 Starting DAPR sidecars..." -ForegroundColor Cyan
    docker-compose -f docker-compose-rabbitmq.yml --profile rabbitmq up -d api-dapr observer-dapr
    if ($LASTEXITCODE -ne 0) { throw "Failed to start DAPR sidecars" }

    Write-Host "⏳ Waiting for DAPR sidecars to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5

    Write-Host "🚀 Starting applications..." -ForegroundColor Cyan
    docker-compose -f docker-compose-rabbitmq.yml --profile rabbitmq up -d webapp api observer
    if ($LASTEXITCODE -ne 0) { throw "Failed to start application services" }

    Write-Host "✅ All services started successfully!" -ForegroundColor Green
} catch {
    Write-Error "❌ Failed to start services: $($_.Exception.Message)"
    Write-Host ""; Write-Host "🔧 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "   - Check logs: docker-compose logs -f" -ForegroundColor Gray
    Write-Host "   - Check individual service: docker-compose logs [service-name]" -ForegroundColor Gray
    Write-Host "   - Restart: docker-compose restart [service-name]" -ForegroundColor Gray
    exit 1
}

Write-Host "⏳ Waiting for services to fully initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 20

# Check RabbitMQ status and restart DAPR if needed
Write-Host "🔍 Checking RabbitMQ connectivity for DAPR..." -ForegroundColor Yellow
$rmqStatus = docker inspect rabbitmq --format "{{.State.Status}}" 2>$null
if ($rmqStatus -eq "running") {
    Write-Host "✅ RabbitMQ is running" -ForegroundColor Green
    $daprStatus = docker inspect api-dapr --format "{{.State.Status}}" 2>$null
    if ($daprStatus -ne "running") { docker restart api-dapr | Out-Null; Start-Sleep -Seconds 5 }
    $observerDaprStatus = docker inspect observer-dapr --format "{{.State.Status}}" 2>$null
    if ($observerDaprStatus -ne "running") { docker restart observer-dapr | Out-Null; Start-Sleep -Seconds 5 }
} else {
    Write-Host "⚠️ RabbitMQ is not running - DAPR may not connect properly" -ForegroundColor Yellow
}

Write-Host ""; Write-Host "📊 Service Status:" -ForegroundColor Green
docker-compose -f docker-compose-rabbitmq.yml ps

Write-Host ""; Write-Host "🌐 Access services at:" -ForegroundColor Green
Write-Host "   🔧 Aspire Dashboard (Telemetry): http://localhost:18888" -ForegroundColor Cyan
Write-Host "   ️ PostgreSQL: localhost:5432" -ForegroundColor Cyan
Write-Host "   🐰 RabbitMQ Management: http://localhost:15672 (admin/admin123)" -ForegroundColor Cyan

Write-Host ""; Write-Host "📱 Applications:" -ForegroundColor Green
Write-Host "   🚀 api (DAPR + RabbitMQ): http://localhost:8081" -ForegroundColor Yellow
Write-Host "   👁️ observer (DAPR + RabbitMQ): http://localhost:8090" -ForegroundColor Yellow
Write-Host "   🌐 webapp (React): http://localhost:8083" -ForegroundColor Yellow

Write-Host ""; Write-Host "🔀 Messaging Configuration:" -ForegroundColor Green
Write-Host "   📨 api → DAPR → RabbitMQ" -ForegroundColor Gray
Write-Host "   📨 observer → DAPR → RabbitMQ" -ForegroundColor Gray

Write-Host ""; Write-Host "🔍 Useful commands:" -ForegroundColor Green
Write-Host "   📋 View logs: docker-compose -f docker-compose-rabbitmq.yml logs -f" -ForegroundColor Gray
Write-Host "   🛑 Stop all: docker-compose -f docker-compose-rabbitmq.yml down" -ForegroundColor Gray
Write-Host "   🔄 Restart service: docker-compose -f docker-compose-rabbitmq.yml restart [service-name]" -ForegroundColor Gray
Write-Host "   📊 Service status: docker-compose -f docker-compose-rabbitmq.yml ps" -ForegroundColor Gray

# Navigate back to root
Set-Location ".."

Write-Host ""; Write-Host "🎉 RabbitMQ environment started successfully!" -ForegroundColor Green

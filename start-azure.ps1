#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Start applications with Azure Service Bus messaging
.DESCRIPTION
    This script starts all applications using Azure Service Bus for messaging.
    - Native API: Uses Azure Service Bus directly
    - Reversible API: Uses DAPR with Azure Service Bus components
.PARAMETER Clean
    Clean up existing containers before starting
.PARAMETER Build
    Force rebuild of containers before starting
.EXAMPLE
    .\start-azure.ps1
    .\start-azure.ps1 -Clean
    .\start-azure.ps1 -Build
#>

param(
    [switch]$Clean,
    [switch]$Build
)

# Set error action preference
$ErrorActionPreference = "Stop"

Write-Host "🚀 Starting applications with Azure Service Bus messaging..." -ForegroundColor Green
Write-Host "   📨 api → DAPR → Azure Service Bus" -ForegroundColor Yellow
Write-Host "   👁️ observer → DAPR → Azure Service Bus" -ForegroundColor Yellow
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


# Verify Dapr secrets.json contains Service Bus connection string
$secretsPath = Join-Path (Get-Location) "dapr\secrets.json"
if (-not (Test-Path $secretsPath)) {
    Write-Error "❌ Missing Dapr secrets file: $secretsPath. Please create it with key 'servicebus-connection-string'."
    exit 1
}

try {
    $secrets = Get-Content $secretsPath | ConvertFrom-Json
} catch {
    Write-Error "❌ Unable to parse $secretsPath as JSON: $($_.Exception.Message)"
    exit 1
}

if (-not $secrets.'servicebus-connection-string') {
    Write-Error "❌ 'servicebus-connection-string' not found in $secretsPath. Dapr Azure Service Bus component requires it."
    exit 1
}
Write-Host "✅ Found Service Bus connection string in secrets.json" -ForegroundColor Green

Write-Host "🌍 Environment: Azure Service Bus" -ForegroundColor Cyan
Write-Host "🔧 DAPR Components: components-azure" -ForegroundColor Cyan

# Clean up if requested
if ($Clean) {
    Write-Host "🧹 Cleaning up existing containers and networks..." -ForegroundColor Yellow
    docker-compose -f docker-compose-azure.yml down --remove-orphans
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
Write-Host "🏁 Starting services with Azure Service Bus..." -ForegroundColor Yellow

try {
    # Start core infrastructure services
    Write-Host "☁️ Starting infrastructure services..." -ForegroundColor Cyan
    docker-compose -f docker-compose-azure.yml up -d postgres aspire-dashboard
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start infrastructure services"
    }
    
    Write-Host "⏳ Waiting for infrastructure to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 10
    
    Write-Host "🔧 Starting DAPR sidecars with Azure Service Bus..." -ForegroundColor Cyan
    docker-compose -f docker-compose-azure.yml up -d api-dapr observer-dapr
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start DAPR sidecars"
    }
    
    Write-Host "⏳ Waiting for DAPR sidecars to initialize..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    Write-Host "🚀 Starting applications..." -ForegroundColor Cyan
    docker-compose -f docker-compose-azure.yml up -d api observer webapp
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start application services"
    }
    
    Write-Host "✅ All services started successfully!" -ForegroundColor Green
    
} catch {
    Write-Error "❌ Failed to start services: $($_.Exception.Message)"
    
    Write-Host ""
    Write-Host "🔧 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "   - Check logs: docker-compose logs -f" -ForegroundColor Gray
    Write-Host "   - Check individual service: docker-compose logs [service-name]" -ForegroundColor Gray
    Write-Host "   - Restart: docker-compose restart [service-name]" -ForegroundColor Gray
    
    exit 1
}

# Wait a moment for services to start
Write-Host "⏳ Waiting for services to initialize..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# Show service status
Write-Host ""
Write-Host "📊 Service Status:" -ForegroundColor Green
docker-compose -f docker-compose-azure.yml ps

Write-Host ""
Write-Host "🌐 Access services at:" -ForegroundColor Green
Write-Host "   🔧 Aspire Dashboard (Telemetry): http://localhost:18888" -ForegroundColor Cyan
Write-Host "   ️ PostgreSQL: localhost:5432" -ForegroundColor Cyan

Write-Host ""
Write-Host "📱 Applications:" -ForegroundColor Green
Write-Host "   🚀 api (DAPR + Azure Service Bus): http://localhost:8081" -ForegroundColor Yellow
Write-Host "   👁️ observer (DAPR + Azure Service Bus): Running on port 8090" -ForegroundColor Yellow
Write-Host "   🌐 webapp (React): http://localhost:8083" -ForegroundColor Yellow

Write-Host ""
Write-Host "☁️ Azure Configuration:" -ForegroundColor Green
Write-Host "   📨 api → DAPR → Azure Service Bus" -ForegroundColor Gray
Write-Host "   📨 observer → DAPR → Azure Service Bus" -ForegroundColor Gray

Write-Host ""
Write-Host "🔍 Useful commands:" -ForegroundColor Green
Write-Host "   📋 View logs: docker-compose -f docker-compose-azure.yml logs -f" -ForegroundColor Gray
Write-Host "   🛑 Stop all: docker-compose -f docker-compose-azure.yml down" -ForegroundColor Gray
Write-Host "   🔄 Restart service: docker-compose -f docker-compose-azure.yml restart [service-name]" -ForegroundColor Gray
Write-Host "   📊 Service status: docker-compose -f docker-compose-azure.yml ps" -ForegroundColor Gray

Write-Host ""
Write-Host "🎉 Azure environment started successfully!" -ForegroundColor Green

# Navigate back to root
Set-Location ".."

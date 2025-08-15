#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Stop all running application containers
.DESCRIPTION
    This script stops all containers and optionally cleans up volumes and networks
.PARAMETER Clean
    Remove containers, volumes, and networks after stopping
.PARAMETER Volumes
    Also remove volumes (use with -Clean)
.EXAMPLE
    .\stop-apps.ps1
    .\stop-apps.ps1 -Clean
    .\stop-apps.ps1 -Clean -Volumes
#>

param(
    [switch]$Clean,
    [switch]$Volumes
)

Write-Host "🛑 Stopping application containers..." -ForegroundColor Yellow

# Navigate to docker directory
Set-Location "docker"

try {
    # Stop all services from both docker-compose files
    Write-Host "🛑 Stopping Azure environment services..." -ForegroundColor Yellow
    docker-compose -f docker-compose-azure.yml down 2>$null

    Write-Host "🛑 Stopping RabbitMQ environment services..." -ForegroundColor Yellow
    docker-compose -f docker-compose-rabbitmq.yml down 2>$null
    docker-compose -f docker-compose-rabbitmq.yml --profile rabbitmq down 2>$null

    if ($Clean) {
        Write-Host "🧹 Cleaning up containers and networks..." -ForegroundColor Yellow
        
        if ($Volumes) {
            Write-Host "📁 Also removing volumes..." -ForegroundColor Yellow
            docker-compose -f docker-compose-azure.yml down --remove-orphans --volumes 2>$null
            docker-compose -f docker-compose-rabbitmq.yml down --remove-orphans --volumes 2>$null
            docker-compose -f docker-compose-rabbitmq.yml --profile rabbitmq down --remove-orphans --volumes 2>$null
            docker system prune -f --volumes
        } else {
            docker-compose -f docker-compose-azure.yml down --remove-orphans 2>$null
            docker-compose -f docker-compose-rabbitmq.yml down --remove-orphans 2>$null
            docker-compose -f docker-compose-rabbitmq.yml --profile rabbitmq down --remove-orphans 2>$null
            docker system prune -f
        }
    }

    Write-Host "✅ Applications stopped successfully!" -ForegroundColor Green

} catch {
    Write-Error "❌ Failed to stop applications: $($_.Exception.Message)"
    exit 1
}

# Navigate back to root
Set-Location ".."

Write-Host ""
Write-Host "💡 To start applications again:" -ForegroundColor Cyan
Write-Host "   🌍 Azure environment: .\start-azure.ps1" -ForegroundColor Gray
Write-Host "   🏠 RabbitMQ environment: .\start-rabbitmq.ps1" -ForegroundColor Gray

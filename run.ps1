# TaskFlow High-Speed Runner
Write-Host "========================================================" -ForegroundColor Cyan
Write-Host "  Starting TaskFlow (High-Speed Mode)" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

$backendDir = Join-Path $PSScriptRoot "backend"
$jarPath = Join-Path $backendDir "target\taskflow-0.0.1-SNAPSHOT.jar"

if (-not (Test-Path $jarPath)) {
    Write-Host "[INFO] JAR not found. Compiling package..." -ForegroundColor Yellow
    Push-Location $backendDir
    & .\mvnw.cmd package -DskipTests
    Pop-Location
}

Write-Host "[INFO] Launching TaskFlow on http://localhost:8081 ..." -ForegroundColor Green
Set-Location $backendDir
java -XX:TieredStopAtLevel=1 -Dspring.profiles.active=dev -jar $jarPath

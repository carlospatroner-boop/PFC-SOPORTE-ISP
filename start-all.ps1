# start-all.ps1 — Levanta todo el sistema SOPORTE de un solo golpe: cluster CockroachDB +
# Kafka/MongoDB + auth-service + ticket-service + ai-service + notification-service +
# report-service + api-gateway + frontend (los 5 microservicios de la Entrega 2 +
# el API Gateway de la Entrega 4, Modulo B + infraestructura).
#
# Requisitos en la maquina donde se corre:
#   - Docker Desktop (con el motor corriendo)
#   - Java 21 + Maven (el comando "mvn" debe funcionar desde una terminal)
#   - Python 3.11+ (el comando "python" y "pip" deben funcionar)
#   - Node.js 20+ (los comandos "node" y "npm" deben funcionar)
#
# Uso (desde la carpeta raiz del proyecto, SOPORTE/):
#   powershell -ExecutionPolicy Bypass -File start-all.ps1
#
# Es seguro volver a correrlo si algo quedo a medias -- los pasos de base de
# datos usan "IF NOT EXISTS" y no borran nada existente. Puede que veas algun
# mensaje de "ya existe" o "ya estaba inicializado": es normal, no es un error.

$ErrorActionPreference = "Continue"
$root = $PSScriptRoot

function Wait-Health($url, $name, $maxTries = 40) {
    for ($i = 0; $i -lt $maxTries; $i++) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) {
                Write-Host "  -> $name listo" -ForegroundColor Green
                return $true
            }
        } catch { Start-Sleep -Seconds 3 }
    }
    Write-Host "  -> $name no respondio a tiempo. Revisa el log en `$env:TEMP." -ForegroundColor Red
    return $false
}

function Wait-CockroachReady($container, $maxTries = 30) {
    for ($i = 0; $i -lt $maxTries; $i++) {
        docker exec $container cockroach sql --insecure -e "SELECT 1;" *> $null
        if ($LASTEXITCODE -eq 0) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

# El contenedor "kafka" queda con estado "Started" en docker mucho antes de que el
# broker interno realmente acepte conexiones de clientes -- si notification-service
# arranca en ese margen, kafkajs se cae con "This server does not host this
# topic-partition" y el consumidor muere en silencio para siempre (visto en vivo).
# Se espera aqui a que el broker responda de verdad antes de seguir.
function Wait-KafkaReady($maxTries = 20) {
    for ($i = 0; $i -lt $maxTries; $i++) {
        docker exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list *> $null
        if ($LASTEXITCODE -eq 0) { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

# En algunas maquinas Windows, "mvn" en el PATH resuelve a un archivo roto/vacio
# (visto en este equipo: C:\windows\system32\mvn, un stray file sin relacion con
# Maven) en vez del "mvn.cmd" real -- Start-Process falla con "%1 no es una
# aplicacion Win32 valida". Se resuelve explicitamente a "mvn.cmd" (la extension
# correcta, que el stray file sin extension no puede eclipsar) y, si tampoco existe
# en el PATH, se cae al wrapper de Maven cacheado por el propio proyecto en
# ~/.m2/wrapper -- asi el script funciona tanto en una maquina con Maven bien
# instalado como en esta.
function Resolve-MvnCommand {
    $cmd = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }

    $wrapper = Get-ChildItem -Path "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-*\*\bin\mvn.cmd" -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($wrapper) { return $wrapper.FullName }

    Write-Host "  (no se encontro mvn.cmd ni el wrapper cacheado; probando 'mvn' tal cual)" -ForegroundColor DarkGray
    return "mvn"
}

$mvnCmd = Resolve-MvnCommand

Write-Host "=== 1/9: Cluster CockroachDB ===" -ForegroundColor Cyan
Set-Location "$root\db-cluster"
docker compose -f docker-compose.cockroach.yml up -d
Write-Host "  Esperando a que el cluster acepte SQL..."
Wait-CockroachReady "roach1" | Out-Null
docker exec roach1 cockroach init --insecure 2>$null
if ($LASTEXITCODE -ne 0) { Write-Host "  (el cluster ya estaba inicializado, sigue normal)" -ForegroundColor DarkGray }

Write-Host "  Cargando esquemas (ticket_db, zonas, auth_db)..."
docker cp scripts\init_db.sql roach1:/init_db.sql
docker exec roach1 cockroach sql --insecure -f /init_db.sql | Out-Null
docker cp config\zones.sql roach1:/zones.sql
docker exec roach1 cockroach sql --insecure -f /zones.sql | Out-Null
docker cp scripts\init_auth_db.sql roach1:/init_auth_db.sql
docker exec roach1 cockroach sql --insecure -f /init_auth_db.sql | Out-Null
docker exec roach1 cockroach sql --insecure -e "ALTER TABLE auth_db.users ADD COLUMN IF NOT EXISTS zone STRING;" | Out-Null
docker cp scripts\init_report_db.sql roach1:/init_report_db.sql
docker exec roach1 cockroach sql --insecure -f /init_report_db.sql | Out-Null
Write-Host "  -> Cluster y esquemas listos" -ForegroundColor Green

Write-Host "`n=== 2/9: Kafka + MongoDB ===" -ForegroundColor Cyan
Set-Location "$root\messaging"
docker compose -f docker-compose.messaging.yml up -d
Write-Host "  Esperando a que el broker de Kafka acepte conexiones..."
if (Wait-KafkaReady) {
    Write-Host "  -> Kafka + MongoDB arriba" -ForegroundColor Green
} else {
    Write-Host "  -> Kafka no respondio a tiempo (notification-service/ai-service podrian fallar al conectarse)" -ForegroundColor Red
}

Write-Host "`n=== 3/9: auth-service ===" -ForegroundColor Cyan
Set-Location "$root\services\auth-service"
Start-Process -FilePath $mvnCmd -ArgumentList "spring-boot:run" `
    -RedirectStandardOutput "$env:TEMP\auth-service.out.log" -RedirectStandardError "$env:TEMP\auth-service.err.log" `
    -WindowStyle Hidden
Wait-Health "http://localhost:8001/actuator/health" "auth-service" | Out-Null

Write-Host "`n=== 4/9: ticket-service ===" -ForegroundColor Cyan
Set-Location "$root\services\svc-principal"
Start-Process -FilePath $mvnCmd -ArgumentList "spring-boot:run" `
    -RedirectStandardOutput "$env:TEMP\ticket-service.out.log" -RedirectStandardError "$env:TEMP\ticket-service.err.log" `
    -WindowStyle Hidden
Wait-Health "http://localhost:8002/actuator/health" "ticket-service" | Out-Null

Write-Host "`n=== 5/9: ai-service ===" -ForegroundColor Cyan
Set-Location "$root\services\ai-service"
pip install -r requirements.txt --quiet
Start-Process -FilePath "python" -ArgumentList "-m", "uvicorn", "app.main:app", "--port", "8004" `
    -RedirectStandardOutput "$env:TEMP\ai-service.out.log" -RedirectStandardError "$env:TEMP\ai-service.err.log" `
    -WindowStyle Hidden
Wait-Health "http://localhost:8004/health" "ai-service" | Out-Null

Write-Host "`n=== 6/9: notification-service ===" -ForegroundColor Cyan
Set-Location "$root\services\notification-service"
npm install --quiet
Start-Process -FilePath "node" -ArgumentList "src/index.js" `
    -RedirectStandardOutput "$env:TEMP\notification-service.out.log" -RedirectStandardError "$env:TEMP\notification-service.err.log" `
    -WindowStyle Hidden
Wait-Health "http://localhost:8003/health" "notification-service" | Out-Null

Write-Host "`n=== 7/9: report-service ===" -ForegroundColor Cyan
Set-Location "$root\services\report-service"
Start-Process -FilePath $mvnCmd -ArgumentList "spring-boot:run" `
    -RedirectStandardOutput "$env:TEMP\report-service.out.log" -RedirectStandardError "$env:TEMP\report-service.err.log" `
    -WindowStyle Hidden
Wait-Health "http://localhost:8005/actuator/health" "report-service" | Out-Null

Write-Host "`n=== 8/9: api-gateway ===" -ForegroundColor Cyan
Set-Location "$root\services\api-gateway"
Start-Process -FilePath $mvnCmd -ArgumentList "spring-boot:run" `
    -RedirectStandardOutput "$env:TEMP\api-gateway.out.log" -RedirectStandardError "$env:TEMP\api-gateway.err.log" `
    -WindowStyle Hidden
Wait-Health "http://localhost:8000/actuator/health" "api-gateway" | Out-Null

Write-Host "`n=== 9/9: Frontend ===" -ForegroundColor Cyan
Set-Location "$root\frontend"
Start-Process -FilePath "python" -ArgumentList "-m", "http.server", "5500" -WindowStyle Hidden
Start-Sleep -Seconds 2
Write-Host "  -> Frontend servido" -ForegroundColor Green

Write-Host "`n=== Creando cuentas de prueba (si no existen ya) ===" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri "http://localhost:8001/api/v1/auth/register" -Method Post -ContentType "application/json" `
        -Body (@{email = "cliente@test.com"; password = "Passw0rd!"; fullName = "Cliente Demo" } | ConvertTo-Json) | Out-Null
    Write-Host "  -> cliente@test.com creado" -ForegroundColor Green
} catch {
    Write-Host "  (cliente@test.com ya existia, sigue normal)" -ForegroundColor DarkGray
}

Set-Location $root

Write-Host "`n================================================================" -ForegroundColor Yellow
Write-Host " TODO LISTO" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host " Abre en el navegador: http://localhost:5500/auth/index.html"
Write-Host ""
Write-Host " Cuentas de prueba:"
Write-Host "   ADMIN:   admin@soporte.local / Admin123!"
Write-Host "   CLIENTE: cliente@test.com / Passw0rd!"
Write-Host ""
Write-Host " Consola CockroachDB: http://localhost:8080"
Write-Host " API Gateway (punto de entrada unico, E4): http://localhost:8000"
Write-Host " report-service (requiere token ADMIN, via gateway): http://localhost:8000/api/v1/reports/summary"
Write-Host " notification-service (demo/depuracion, via gateway): http://localhost:8000/api/v1/notifications"
Write-Host " Logs de cada servicio en: `$env:TEMP\<servicio>.out.log"
Write-Host "================================================================"

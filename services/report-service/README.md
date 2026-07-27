# report-service

Microservicio de reportes del PFC (equipo ACC — Soporte Técnico ISP). Java 21 + Spring Boot 3.2,
conectado por JDBC a su propia base `report_db` en el cluster CockroachDB del proyecto.

## Responsabilidad

Lado de **consulta (query)** del patrón CQRS: mantiene una tabla de lectura desnormalizada
(`ticket_summary`) reconstruida únicamente a partir de los eventos de Kafka que publican
`ticket-service` y `ai-service` (`ticket.created`, `ticket.classified`, `ticket.status-changed`,
`ticket.assigned` — ver `config/ReportEventListener.java`). Nunca consulta `ticket_db`
directamente: está completamente desacoplado del lado transaccional, lo que permite responder
consultas de agregación/reportería sin competir por recursos con la escritura de tickets.

## Cómo correrlo

### 1. Esquema de base de datos

```bash
cd ../../db-cluster
# usando el mismo patron que init_db.sql / init_auth_db.sql (ver README de db-cluster)
docker exec -i roach1 cockroach sql --insecure < scripts/init_report_db.sql
```

### 2. Infraestructura (Kafka, ya debe estar arriba para ticket-service/ai-service)

```bash
cd ../messaging
docker compose -f docker-compose.messaging.yml up -d
```

### 3. Compilar y correr

```bash
cd services/report-service
mvn spring-boot:run
```

El servicio queda en `http://localhost:8005` y arranca 4 `@KafkaListener` en segundo plano.

## Endpoints (todos requieren un token ADMIN — ver Autenticación)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/reports/summary` | Conteos agregados: total, por estado, por zona, por categoría |
| GET | `/api/v1/reports/tickets?zone=&status=&category=` | Lista filtrable del modelo de lectura |
| GET | `/api/v1/reports/export.csv?zone=&status=&category=` | Exportación CSV de la misma lista |

## Autenticación (ADMIN únicamente)

`config/AuthGatewayFilter.java` exige `Authorization: Bearer <token>` en cada request a
`/api/v1/reports/**`, valida el token contra `auth-service` (`GET /validate`, mismo enfoque que
`ticket-service`) y además **rechaza con 403 cualquier rol distinto de ADMIN** — reportería es una
función de gestión, coherente con la matriz de autorización ya establecida en `ticket-service`.

```bash
curl http://localhost:8005/api/v1/reports/summary \
  -H "Authorization: Bearer <token-de-un-usuario-ADMIN>"
```

## Variables de entorno

| Variable | Default |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:26257/report_db?sslmode=disable` |
| `DB_USER` / `DB_PASS` | `root` / (vacío) |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `AUTH_SERVICE_URL` | `http://localhost:8001/api/v1/auth` |

## Tests

```bash
mvn test
```

Pruebas unitarias puras (sin Spring context ni cluster real, misma convención del resto del
proyecto): `ReportEventListenerTest` (cada evento actualiza los campos correctos del modelo de
lectura, con `TicketSummaryRepository` mockeado) y `CsvExporterTest` (formato/escapado del CSV).

## Limitación conocida

`/api/v1/reports/tickets` y `/export.csv` filtran en memoria sobre `findAll()` en vez de consultas
derivadas combinadas por cada subconjunto de filtros — aceptable para el volumen de datos de un
PFC, pero no pensado para escalar a un dataset de producción grande.

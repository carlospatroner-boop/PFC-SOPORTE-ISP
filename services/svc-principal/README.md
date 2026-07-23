# ticket-service (svc-principal)

Microservicio central del PFC (equipo ACC — Soporte Técnico ISP). Java 21 + Spring Boot 3.2,
conectado por JDBC al cluster CockroachDB del proyecto (carpeta `db-cluster/` en la raíz del repo).

## Cómo correrlo en VS Code

1. Instalar extensiones: **Extension Pack for Java** y **Spring Boot Extension Pack**.
2. Abrir esta carpeta (`services/svc-principal`) o el repo completo en VS Code.
3. Levantar primero el cluster CockroachDB (ver `../../db-cluster/README.md`) y cargar el esquema
   (`init_db.sql`, `zones.sql`, `seed_partitioned.sql`).
4. Correr `TicketServiceApplication.java` con el botón "Run" que aparece sobre el `main`, o:
   ```bash
   mvn spring-boot:run
   ```
5. El servicio queda en `http://localhost:8002`.

## Endpoints (contrato REST heredado de la Entrega 2, Capítulo 7.3)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/tickets` | Crear ticket (requiere `zone` y `clientId` en el body) |
| GET | `/api/v1/tickets?zone=&status=` | Listar tickets, filtro opcional por zona y/o estado |
| GET | `/api/v1/tickets/{zone}/{id}` | Detalle de un ticket |
| PATCH | `/api/v1/tickets/{zone}/{id}/status` | Cambiar estado |
| POST | `/api/v1/tickets/{zone}/{id}/assign?technicianId=` | Asignar técnico |

Ejemplo de creación:

```bash
curl -X POST http://localhost:8002/api/v1/tickets \
  -H "Content-Type: application/json" \
  -d '{
    "zone": "QUEVEDO_NORTE",
    "clientId": "11111111-1111-1111-1111-111111111111",
    "title": "Sin acceso a Internet",
    "description": "El router muestra luz roja desde las 08:00",
    "contactPhone": "0991234567",
    "address": "Av. Quevedo 123"
  }'
```

## Pendiente (fuera del alcance de este esqueleto)

- Integración real con `auth-service` (JWT) vía API Gateway — por ahora `clientId` se recibe
  directo en el request, no desde un token.
- Publicación de eventos a Kafka (`ticket.created`, etc. — Capítulo 5 y 6 de la E2). Se puede
  agregar con `spring-kafka` cuando el equipo retome esa parte.
- Verificar que el dialecto de Hibernate funciona sin fricción contra CockroachDB (algunas
  features avanzadas de PostgreSQL no están soportadas 1:1; para el CRUD básico de este servicio
  no debería haber problema, pero conviene probarlo apenas el cluster esté arriba).

## Nota sobre verificación

Este esqueleto fue generado con asistencia de IA (ver `/ai-usage-declaration.md` en la raíz del
repo) y **no pudo compilarse en el entorno donde se generó** porque no tiene acceso a Maven
Central. Antes de dar por buena esta base, correr en tu máquina:

```bash
mvn clean test      # corre TicketServiceTest (no requiere el cluster levantado)
mvn clean package   # build completo
```

y reportar cualquier error de compilación para corregirlo.

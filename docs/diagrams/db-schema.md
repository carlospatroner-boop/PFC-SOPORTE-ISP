# Esquema de `ticket_db` (CockroachDB, cluster de 3 nodos)

Corresponde a `db-cluster/scripts/init_db.sql`. `tickets` es la tabla de mayor cardinalidad y la
única fragmentada (`PARTITION BY RANGE (created_at)`, ver
[ADR-0003](../adr/0003-sharding-policy.md)); `technicians` es una dimensión pequeña sin
particionar.

```mermaid
erDiagram
    TICKETS {
        TIMESTAMPTZ created_at PK "particion: RANGE, 4 trimestres 2026"
        UUID id PK "indice unico secundario tickets_id_key"
        STRING zone "indexado (idx_tickets_zone), ya NO es la clave de fragmentacion"
        UUID client_id "indexado implicitamente via findByClientId"
        UUID technician_id FK "nullable, referencia technicians(id)"
        STRING category "nullable, la completa ai-service via Kafka"
        STRING priority "nullable, la completa ai-service via Kafka"
        STRING status "NUEVO por defecto, indexado (idx_tickets_status)"
        STRING description
        TIMESTAMPTZ sla_deadline
        TIMESTAMPTZ resolved_at
        BOOL sla_breached
    }

    TECHNICIANS {
        UUID id PK "gen_random_uuid()"
        STRING full_name
        STRING zone "zona donde opera (no relacionado a la particion de tickets)"
        STRING specialty
        BOOL active
    }

    NETWORK_INCIDENTS_SUMMARY {
        STRING zone PK
        TIMESTAMPTZ period_hour PK
        STRING incident_type PK
        INT8 incident_count
        FLOAT8 avg_resolution_min
        FLOAT8 mttr_min
    }

    TICKETS }o--o| TECHNICIANS : "technician_id -> id"
```

## Notas de diseño

- **Colocalización con `clientes`**: la guía de la Entrega 3 pide colocalizar `tickets` con
  `clientes` por `client_id`. Los clientes viven en `auth_db.users`, propiedad de `auth-service`
  — una base de datos física distinta, por diseño de microservicios (cada servicio es dueño de su
  propio esquema). Ver la sección correspondiente del ADR-0003 para la discusión completa de este
  trade-off.
- **`network_incidents_summary`**: tabla de respaldo para materializar el resultado del pipeline
  de Spark (ver `spark/README.md`, sección de integración) — no se llena desde `ticket-service`,
  la llena un job de Spark o un script de materialización aparte.
- El esquema completo de los otros 4 microservicios (`auth_db`, `report_db`, `ai_db`,
  `notifications_db`) vive en sus propios scripts: `db-cluster/scripts/init_auth_db.sql`,
  `db-cluster/scripts/init_report_db.sql`, y las colecciones de MongoDB documentadas en los
  READMEs de `ai-service`/`notification-service` respectivamente.

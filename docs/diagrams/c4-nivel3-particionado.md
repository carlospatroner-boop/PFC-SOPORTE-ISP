# C4 Nivel 3 — Componentes: partición de `tickets` en CockroachDB

Zoom al cluster CockroachDB (`ticket_service`), mostrando la estrategia real de fragmentación
tras la Entrega 3: **por fecha de apertura** (`PARTITION BY RANGE`), no por zona geográfica — ver
[`docs/adr/0003-sharding-policy.md`](../adr/0003-sharding-policy.md) para la justificación completa
del cambio y sus trade-offs.

```mermaid
graph TB
    subgraph App["ticket-service (Java 21 + Spring Boot)"]
        TS[TicketService]
        TR["TicketRepository<br/>findByTicketId(id)"]
        TS --> TR
    end

    subgraph Cluster["Cluster CockroachDB (3 nodos, replicación factor 3)"]
        subgraph Table["tabla tickets — PK (created_at, id)"]
            Q1["Partición tickets_2026_q1<br/>created_at < 2026-04-01"]
            Q2["Partición tickets_2026_q2<br/>2026-04-01 a 2026-07-01"]
            Q3["Partición tickets_2026_q3<br/>2026-07-01 a 2026-10-01"]
            Q4["Partición tickets_2026_q4<br/>created_at >= 2026-10-01"]
        end
        IDX["Índice único secundario<br/>tickets_id_key (id)<br/>-- resuelve el lookup por id sin<br/>conocer created_at de antemano"]
        ZIDX["Índice secundario<br/>idx_tickets_zone (zone)<br/>-- respalda el RBAC de TECNICO<br/>y los reportes por zona"]
    end

    TR -->|"lookup por id"| IDX
    IDX -.->|"apunta a la fila real"| Q1
    IDX -.-> Q2
    IDX -.-> Q3
    IDX -.-> Q4
    TR -->|"filtros por zona (scatter-gather<br/>entre las 4 particiones)"| ZIDX

    style Q1 fill:#4f46e5,color:#fff
    style Q2 fill:#4f46e5,color:#fff
    style Q3 fill:#4f46e5,color:#fff
    style Q4 fill:#4f46e5,color:#fff
    style IDX fill:#06b6d4,color:#fff
    style ZIDX fill:#f59e0b,color:#fff
```

**Puntos clave (ver ADR-0003 para el detalle):**

- `zone` ya **no** es la clave de fragmentación — sigue existiendo como columna de negocio real
  (RBAC de TECNICO, reportes), respaldada por un índice secundario normal, no por partición física.
- El punto de acceso más común del sistema (buscar un ticket por `id`) pasa por el índice único
  `tickets_id_key`, no por la clave primaria compuesta directamente.
- Las consultas por rango de fecha (la mayoría de los reportes reales) se resuelven dentro de 1-2
  particiones contiguas; las consultas filtradas por zona ahora cruzan las 4 particiones
  (*scatter-gather*), un trade-off documentado y medido en `db-cluster/scripts/queries_bench.sql`.

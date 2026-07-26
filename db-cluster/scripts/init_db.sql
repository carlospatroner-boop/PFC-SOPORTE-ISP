-- init_db.sql
-- Esquema distribuido para ticket-service (equipo ACC — Soporte Técnico ISP)
-- Ejecutar con: cockroach sql --insecure --host=localhost:26257 -f init_db.sql

CREATE DATABASE IF NOT EXISTS ticket_db;
SET DATABASE = ticket_db;

-- Tabla de técnicos (dimensión pequeña, no particionada)
CREATE TABLE IF NOT EXISTS technicians (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name STRING NOT NULL,
    zone STRING NOT NULL,          -- zona donde opera el técnico
    specialty STRING,
    active BOOL DEFAULT TRUE
);

-- Tabla principal de tickets, particionada horizontalmente por fecha de apertura
-- (fecha_apertura -> created_at), segun exige la Guia de Entrega 3 (Tabla 1, fila
-- ACC) -- reemplaza el esquema anterior por zona geografica (ver ADR-0003).
-- "created_at" debe ser la columna lider de la PK para que CockroachDB pueda
-- aplicar PARTITION BY RANGE sobre ella.
CREATE TABLE IF NOT EXISTS tickets (
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    zone            STRING NOT NULL,          -- 'QUEVEDO_CENTRO' | 'QUEVEDO_NORTE' | 'QUEVEDO_SUR' (ya no es la clave de fragmentacion, ver ADR-0003)
    client_id       UUID NOT NULL,
    technician_id   UUID REFERENCES technicians(id),
    category        STRING,                   -- CONECTIVIDAD | DNS | HARDWARE | CONFIGURACION | VELOCIDAD
    priority        STRING,                   -- CRITICO | ALTO | MEDIO | BAJO
    status          STRING NOT NULL DEFAULT 'NUEVO',
    description     STRING,
    sla_deadline    TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ,
    sla_breached    BOOL DEFAULT FALSE,
    PRIMARY KEY (created_at, id)
) PARTITION BY RANGE (created_at) (
    PARTITION tickets_2026_q1 VALUES FROM (MINVALUE) TO ('2026-04-01T00:00:00Z'),
    PARTITION tickets_2026_q2 VALUES FROM ('2026-04-01T00:00:00Z') TO ('2026-07-01T00:00:00Z'),
    PARTITION tickets_2026_q3 VALUES FROM ('2026-07-01T00:00:00Z') TO ('2026-10-01T00:00:00Z'),
    PARTITION tickets_2026_q4 VALUES FROM ('2026-10-01T00:00:00Z') TO (MAXVALUE)
);

-- Punto de acceso mas comun del sistema: buscar un ticket por su id, sin conocer
-- su fecha_apertura de antemano (el id ya no es autosuficiente como PK -- ver
-- ADR-0003). Indice unico secundario para que ese lookup no tenga que escanear
-- las 4 particiones.
CREATE UNIQUE INDEX IF NOT EXISTS tickets_id_key ON tickets (id);

-- La zona sigue siendo un filtro de negocio real (RBAC de TECNICO, reportes por
-- zona) aunque ya no sea la clave de fragmentacion -- se indexa para que ese
-- filtro no degrade a un full scan.
CREATE INDEX IF NOT EXISTS idx_tickets_zone ON tickets (zone);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets (status);

-- Tabla de incidencias de red (telemetria), respaldo del reporte agregado que
-- produce el pipeline Spark (Paso 8 — integracion). No se carga aqui; la llena
-- el job de Spark o un script de materializacion posterior.
CREATE TABLE IF NOT EXISTS network_incidents_summary (
    zone                STRING NOT NULL,
    period_hour         TIMESTAMPTZ NOT NULL,
    incident_type       STRING NOT NULL,
    incident_count      INT8 NOT NULL,
    avg_resolution_min  FLOAT8,
    mttr_min            FLOAT8,
    PRIMARY KEY (zone, period_hour, incident_type)
);

-- Verificacion rapida:
--   SHOW PARTITIONS FROM TABLE tickets;
--   SHOW CREATE TABLE tickets;

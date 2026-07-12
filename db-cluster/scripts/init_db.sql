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

-- Tabla principal de tickets, particionada horizontalmente por zona geografica.
-- La zona debe formar parte de la PK (o de un indice) para que CockroachDB
-- pueda aplicar PARTITION BY LIST sobre ella.
CREATE TABLE IF NOT EXISTS tickets (
    zone            STRING NOT NULL,          -- 'QUEVEDO_CENTRO' | 'QUEVEDO_NORTE' | 'QUEVEDO_SUR'
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    client_id       UUID NOT NULL,
    technician_id   UUID REFERENCES technicians(id),
    category        STRING,                   -- CONECTIVIDAD | DNS | HARDWARE | CONFIGURACION | VELOCIDAD
    priority        STRING,                   -- CRITICO | ALTO | MEDIO | BAJO
    status          STRING NOT NULL DEFAULT 'NUEVO',
    description     STRING,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    sla_deadline    TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ,
    sla_breached    BOOL DEFAULT FALSE,
    PRIMARY KEY (zone, id)
) PARTITION BY LIST (zone) (
    PARTITION tickets_centro VALUES IN ('QUEVEDO_CENTRO'),
    PARTITION tickets_norte  VALUES IN ('QUEVEDO_NORTE'),
    PARTITION tickets_sur    VALUES IN ('QUEVEDO_SUR'),
    PARTITION tickets_default VALUES IN (DEFAULT)
);

-- Indice secundario util para el analisis comparativo (Paso 5): busquedas por fecha
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets (created_at);
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

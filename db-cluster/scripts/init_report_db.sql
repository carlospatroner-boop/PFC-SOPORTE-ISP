-- Base de datos propia de report-service (lado de lectura del CQRS), equipo ACC.
-- Una fila por ticket, reconstruida unicamente a partir de los eventos de Kafka
-- publicados por ticket-service/ai-service (ticket.created, ticket.classified,
-- ticket.status-changed, ticket.assigned) -- ver config/ReportEventListener.java.
-- Completamente desacoplada de ticket_db.tickets: report-service nunca la consulta
-- ni depende de su esquema, solo de los eventos publicos.

CREATE DATABASE IF NOT EXISTS report_db;
SET DATABASE = report_db;

CREATE TABLE IF NOT EXISTS ticket_summary (
    zone           STRING NOT NULL,
    ticket_id      UUID NOT NULL,
    client_id      UUID,
    technician_id  UUID,
    category       STRING,
    priority       STRING,
    status         STRING NOT NULL,
    description    STRING,
    created_at     TIMESTAMPTZ,
    updated_at     TIMESTAMPTZ,
    PRIMARY KEY (zone, ticket_id)
);

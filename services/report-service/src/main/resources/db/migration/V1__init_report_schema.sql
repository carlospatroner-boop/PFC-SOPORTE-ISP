-- V1__init_report_schema.sql
-- Esquema de report-service (equipo ACC -- Soporte Tecnico ISP), aplicado por Flyway al
-- arrancar el servicio. Reemplaza a db-cluster/scripts/init_report_db.sql (Entregable 5 de la
-- guia de cierre: aquel guion no era una migracion versionada, solo un script suelto que
-- "db-init" corria contra el cluster). La base de datos "report_db" en si la sigue creando
-- "db-init" en docker-compose.yml, antes de que este servicio arranque -- Flyway conecta a una
-- base que ya existe, no la crea.
--
-- Lado de lectura del CQRS: una fila por ticket, reconstruida unicamente a partir de los
-- eventos de Kafka publicados por ticket-service/ai-service (ticket.created,
-- ticket.classified, ticket.status-changed, ticket.assigned) -- ver
-- config/ReportEventListener.java. Completamente desacoplada de ticket_db.tickets:
-- report-service nunca la consulta ni depende de su esquema, solo de los eventos publicos.

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

-- seed_partitioned.sql
-- Carga de datos de ejemplo respetando la distribucion 40/30/30 entre zonas
-- (equivalente al 60/40 sugerido por la guia, adaptado a 3 particiones).
-- Ejecutar: cockroach sql --insecure --host=localhost:26257 -f seed_partitioned.sql

SET DATABASE = ticket_db;

INSERT INTO technicians (full_name, zone, specialty) VALUES
    ('Ana Morales',    'QUEVEDO_CENTRO', 'CONECTIVIDAD'),
    ('Luis Zambrano',  'QUEVEDO_CENTRO', 'HARDWARE'),
    ('Maria Cedeño',   'QUEVEDO_NORTE',  'DNS'),
    ('Pedro Vera',     'QUEVEDO_NORTE',  'CONFIGURACION'),
    ('Sofia Intriago', 'QUEVEDO_SUR',    'VELOCIDAD'),
    ('Jorge Alcivar',  'QUEVEDO_SUR',    'CONECTIVIDAD');

-- ~40% centro, ~30% norte, ~30% sur, generado con generate_series para volumen rapido de prueba.
INSERT INTO tickets (zone, client_id, category, priority, status, description, created_at, sla_deadline)
SELECT
    CASE
        WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO'
        WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE'
        ELSE 'QUEVEDO_SUR'
    END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    now() - (i || ' minutes')::INTERVAL,
    now() - (i || ' minutes')::INTERVAL + INTERVAL '4 hours'
FROM generate_series(1, 20000) AS i;

-- Verificacion de distribucion real por particion:
--   SELECT zone, count(*) FROM tickets GROUP BY zone ORDER BY zone;

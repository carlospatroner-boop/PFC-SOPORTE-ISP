-- seed_partitioned_batched.sql
-- Version segura de seed_partitioned.sql: la misma carga de datos sinteticos
-- distribuidos a lo largo de 2026, pero en LOTES de 10,000 filas en vez de una
-- sola transaccion de 150,000 -- la causa raiz documentada del incidente de OOM
-- de este proyecto (ver docker-compose.cockroach.yml, comentario sobre mem_limit).
--
-- Cada bloque es una sentencia INSERT independiente (su propia transaccion
-- implicita), por lo que un fallo a mitad de carga no requiere reiniciar desde
-- cero: solo hay que ajustar el rango de "i" del siguiente bloque.
--
-- Ejecutar UNO POR UNO (no todo el archivo de una vez), verificando el conteo
-- entre cada bloque:
--   cockroach sql --insecure --host=localhost:26257 -f seed_partitioned_batched.sql
-- o copiando cada bloque a mano en una sesion de "cockroach sql" interactiva.

SET DATABASE = ticket_db;

-- Tecnicos (idempotente si ya existen, se puede saltar si seed_partitioned.sql
-- ya se corrio antes)
INSERT INTO technicians (full_name, zone, specialty)
SELECT * FROM (VALUES
    ('Ana Morales',    'QUEVEDO_CENTRO', 'CONECTIVIDAD'),
    ('Luis Zambrano',  'QUEVEDO_CENTRO', 'HARDWARE'),
    ('Maria Cedeño',   'QUEVEDO_NORTE',  'DNS'),
    ('Pedro Vera',     'QUEVEDO_NORTE',  'CONFIGURACION'),
    ('Sofia Intriago', 'QUEVEDO_SUR',    'VELOCIDAD'),
    ('Jorge Alcivar',  'QUEVEDO_SUR',    'CONECTIVIDAD')
) AS v(full_name, zone, specialty)
WHERE NOT EXISTS (SELECT 1 FROM technicians WHERE technicians.full_name = v.full_name);

-- ============================================================
-- LOTE 1 de 8 -- filas 1 a 10,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(1, 10000) AS i;

-- Verificar antes de seguir: SELECT count(*) FROM tickets;  (debe subir de a 10,000)

-- ============================================================
-- LOTE 2 de 8 -- filas 10,001 a 20,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(10001, 20000) AS i;

-- ============================================================
-- LOTE 3 de 8 -- filas 20,001 a 30,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(20001, 30000) AS i;

-- ============================================================
-- LOTE 4 de 8 -- filas 30,001 a 40,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(30001, 40000) AS i;

-- ============================================================
-- LOTE 5 de 8 -- filas 40,001 a 50,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(40001, 50000) AS i;

-- ============================================================
-- LOTE 6 de 8 -- filas 50,001 a 60,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(50001, 60000) AS i;

-- ============================================================
-- LOTE 7 de 8 -- filas 60,001 a 70,000
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(60001, 70000) AS i;

-- ============================================================
-- LOTE 8 de 8 -- filas 70,001 a 80,000
-- Sumado a los ~20,000 ya existentes, deja la tabla en ~100,000+ filas,
-- cumpliendo el minimo de 10^5 del Modulo D.
-- ============================================================
INSERT INTO tickets (created_at, zone, client_id, category, priority, status, description, sla_deadline)
SELECT
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds'),
    CASE WHEN (i % 10) < 4 THEN 'QUEVEDO_CENTRO' WHEN (i % 10) < 7 THEN 'QUEVEDO_NORTE' ELSE 'QUEVEDO_SUR' END,
    gen_random_uuid(),
    (ARRAY['CONECTIVIDAD','DNS','HARDWARE','CONFIGURACION','VELOCIDAD'])[1 + (i % 5)],
    (ARRAY['CRITICO','ALTO','MEDIO','BAJO'])[1 + (i % 4)],
    (ARRAY['NUEVO','ASIGNADO','EN_PROGRESO','RESUELTO','CERRADO'])[1 + (i % 5)],
    'Ticket sintetico de carga inicial #' || i,
    TIMESTAMPTZ '2026-01-01 00:00:00+00' + (i * INTERVAL '210 seconds') + INTERVAL '4 hours'
FROM generate_series(70001, 80000) AS i;

-- Verificacion final:
--   SELECT count(*) FROM tickets;  -- debe ser >= 100,000
--   SHOW PARTITIONS FROM TABLE tickets;

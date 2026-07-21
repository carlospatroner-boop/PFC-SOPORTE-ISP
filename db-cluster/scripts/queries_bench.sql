-- queries_bench.sql
-- 5 consultas representativas para el Paso 5 (analisis comparativo de rendimiento).
-- Ejecutar cada una con EXPLAIN ANALYZE en:
--   (a) el cluster de 3 nodos (puerto 26257)
--   (b) una instancia unica: cockroach start-single-node --insecure --listen-addr=localhost:26260
-- y volcar tiempo, filas leidas y filas devueltas a la tabla comparativa del documento.

SET DATABASE = ticket_db;

-- Q1: Lectura por clave primaria (zone, id) — punto de acceso mas comun del sistema
EXPLAIN ANALYZE
SELECT * FROM tickets WHERE zone = 'QUEVEDO_NORTE' AND id = (
    SELECT id FROM tickets WHERE zone = 'QUEVEDO_NORTE' LIMIT 1
);

-- Q2: Consulta de rango — tickets abiertos en las ultimas 24 horas por zona
EXPLAIN ANALYZE
SELECT id, category, priority, status, created_at
FROM tickets
WHERE zone = 'QUEVEDO_SUR' AND created_at >= now() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Q3: Consulta de rango entre fechas, sin filtro de zona (cruza particiones)
EXPLAIN ANALYZE
SELECT zone, count(*) AS total
FROM tickets
WHERE created_at BETWEEN now() - INTERVAL '7 days' AND now()
GROUP BY zone;

-- Q4: Agregacion — SLA breach rate por zona y categoria
EXPLAIN ANALYZE
SELECT zone, category,
       count(*) AS total_tickets,
       sum(CASE WHEN sla_breached THEN 1 ELSE 0 END) AS breaches,
       round(100.0 * sum(CASE WHEN sla_breached THEN 1 ELSE 0 END) / count(*), 2) AS breach_pct
FROM tickets
GROUP BY zone, category
ORDER BY breach_pct DESC;

-- Q5: Join — tickets con datos del tecnico asignado
EXPLAIN ANALYZE
SELECT t.id, t.zone, t.category, t.status, tech.full_name, tech.specialty
FROM tickets t
JOIN technicians tech ON t.technician_id = tech.id
WHERE t.status IN ('ASIGNADO', 'EN_PROGRESO')
LIMIT 500;

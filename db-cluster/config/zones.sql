-- zones.sql
-- Politica de replicacion por localidad (Paso 3 de la guia).
-- Cada particion se ancla preferentemente al nodo cuya locality coincide con su zona,
-- pero mantiene 3 replicas (una en cada nodo) para garantizar tolerancia a fallos
-- ante la caida de cualquier nodo individual.
--
-- Ejecutar despues de init_db.sql:
--   cockroach sql --insecure --host=localhost:26257 -f zones.sql

SET DATABASE = ticket_db;

ALTER PARTITION tickets_centro OF TABLE tickets
    CONFIGURE ZONE USING num_replicas = 3, constraints = '{"+zone=quevedo-centro": 1}';

ALTER PARTITION tickets_norte OF TABLE tickets
    CONFIGURE ZONE USING num_replicas = 3, constraints = '{"+zone=quevedo-norte": 1}';

ALTER PARTITION tickets_sur OF TABLE tickets
    CONFIGURE ZONE USING num_replicas = 3, constraints = '{"+zone=quevedo-sur": 1}';

-- Verificacion:
--   SHOW ZONE CONFIGURATION FOR PARTITION tickets_norte OF TABLE tickets;
--   SELECT * FROM crdb_internal.zones WHERE target LIKE '%tickets%';

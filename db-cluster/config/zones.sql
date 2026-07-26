-- zones.sql
-- Politica de replicacion (Paso 3 de la guia / D1.2 de la rubrica de Entrega 3).
--
-- Desde que la fragmentacion de "tickets" paso de ser por zona geografica a ser
-- por fecha_apertura (ver ADR-0003 y init_db.sql), ya no existe una correspondencia
-- 1:1 entre una particion y la locality de un nodo especifico -- una particion
-- trimestral puede (y debe) tener tickets de las 3 zonas geograficas mezclados.
-- Por eso ya no se ancla cada particion a un nodo por constraint de locality;
-- simplemente se exige el factor de replicacion 3 (quorum de escritura = 2 de 3)
-- de forma uniforme sobre toda la tabla, para tolerar la caida de cualquier nodo
-- individual sin perder disponibilidad de escritura.
--
-- Ejecutar despues de init_db.sql:
--   cockroach sql --insecure --host=localhost:26257 -f zones.sql

SET DATABASE = ticket_db;

ALTER TABLE tickets CONFIGURE ZONE USING num_replicas = 3;

-- Verificacion:
--   SHOW ZONE CONFIGURATION FOR TABLE tickets;
--   SELECT * FROM crdb_internal.zones WHERE target LIKE '%tickets%';

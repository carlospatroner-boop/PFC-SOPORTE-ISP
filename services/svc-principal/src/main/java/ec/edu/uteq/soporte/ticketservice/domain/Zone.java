package ec.edu.uteq.soporte.ticketservice.domain;

/**
 * Zonas geograficas usadas como clave de fragmentacion horizontal en CockroachDB
 * (ver db-cluster/scripts/init_db.sql -> PARTITION BY LIST (zone), y docs/adr/0003-sharding-policy.md).
 * Debe mantenerse en sincronia con las particiones y las localities de los nodos.
 */
public enum Zone {
    QUEVEDO_CENTRO,
    QUEVEDO_NORTE,
    QUEVEDO_SUR
}

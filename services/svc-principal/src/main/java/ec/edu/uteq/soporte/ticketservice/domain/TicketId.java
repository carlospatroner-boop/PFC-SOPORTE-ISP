package ec.edu.uteq.soporte.ticketservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave primaria compuesta (zone, id) de Ticket, requerida por JPA (@IdClass) para
 * reflejar la clave primaria real de la tabla en CockroachDB:
 *   PRIMARY KEY (zone, id)
 * definida en db-cluster/scripts/init_db.sql. La columna "zone" debe ser el primer
 * componente para que el PARTITION BY LIST (zone) sea efectivo (ver ADR-0003).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketId implements Serializable {
    private Zone zone;
    private UUID id;
}

package ec.edu.uteq.soporte.ticketservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Clave primaria compuesta (created_at, id) de Ticket, requerida por JPA
 * (@IdClass) para reflejar la clave primaria real de la tabla en CockroachDB:
 *   PRIMARY KEY (created_at, id)
 * definida en db-cluster/scripts/init_db.sql. "created_at" debe ser el primer
 * componente para que el PARTITION BY RANGE (created_at) sea efectivo (ver
 * ADR-0003). La aplicacion casi nunca busca un ticket por esta clave completa --
 * el punto de acceso real es TicketRepository.findByTicketId(UUID), que usa el
 * indice unico secundario sobre "id" (ver Ticket.java).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketId implements Serializable {
    private OffsetDateTime createdAt;
    private UUID id;
}

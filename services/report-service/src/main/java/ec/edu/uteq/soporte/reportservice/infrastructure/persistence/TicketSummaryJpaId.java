package ec.edu.uteq.soporte.reportservice.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave primaria compuesta (zone, ticketId) de TicketSummaryJpaEntity, requerida por
 * JPA (@IdClass) para reflejar PRIMARY KEY (zone, ticket_id) de
 * db-cluster/scripts/init_report_db.sql. Detalle puramente tecnico de JPA -- por eso
 * vive en infrastructure/persistence y no en el dominio (a diferencia de la Entrega
 * anterior, donde vivia junto a la entidad en domain/).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryJpaId implements Serializable {
    private String zone;
    private UUID ticketId;
}

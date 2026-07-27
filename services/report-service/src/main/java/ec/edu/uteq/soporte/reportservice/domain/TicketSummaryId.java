package ec.edu.uteq.soporte.reportservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave primaria compuesta (zone, ticketId) de TicketSummary, requerida por JPA
 * (@IdClass) para reflejar PRIMARY KEY (zone, ticket_id) de
 * db-cluster/scripts/init_report_db.sql.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryId implements Serializable {
    private String zone;
    private UUID ticketId;
}

package ec.edu.uteq.soporte.reportservice.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave primaria compuesta (zone, ticketId) de TicketSummary, requerida por JPA
 * (@IdClass) para reflejar PRIMARY KEY (zone, ticket_id) de
 * services/report-service/src/main/resources/db/migration/V1__init_report_schema.sql.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryId implements Serializable {
    private String zone;
    private UUID ticketId;
}

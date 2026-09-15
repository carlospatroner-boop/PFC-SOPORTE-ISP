package ec.edu.uteq.soporte.reportservice.infrastructure.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

/**
 * Clave primaria compuesta (zone, ticketId) de TicketSummaryJpaEntity, requerida por
 * JPA (@IdClass) para reflejar PRIMARY KEY (zone, ticket_id) de
 * services/report-service/src/main/resources/db/migration/V1__init_report_schema.sql. Detalle puramente tecnico de JPA -- por eso
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

package ec.edu.uteq.soporte.reportservice.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad de dominio de solo lectura (lado query del CQRS) -- deliberadamente SIN
 * anotaciones JPA (mismo criterio que Ticket en ticket-service y User en
 * auth-service, ver ADR-0005). Se reconstruye unicamente a partir de los eventos de
 * Kafka que publican ticket-service/ai-service -- ver
 * infrastructure/messaging/ReportEventListener.java -- nunca desde una consulta
 * directa a ticket_db. El mapeo real vive en
 * infrastructure/persistence/TicketSummaryJpaEntity.java.
 *
 * zone/category/priority/status se guardan como String plano, no como los enums
 * Zone/Category/Priority/TicketStatus de ticket-service: este servicio no valida el
 * vocabulario, solo lo refleja tal cual llega en el evento. Si ticket-service agrega
 * un valor nuevo maniana, report-service lo almacena sin romperse (fail-open), a
 * diferencia del lado transaccional donde un valor invalido si debe rechazarse.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSummary {

    private String zone;
    private UUID ticketId;
    private UUID clientId;
    private UUID technicianId;
    private String category;
    private String priority;
    private String status;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

package ec.edu.uteq.soporte.reportservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entidad de solo lectura (lado query del CQRS), mapeada a `ticket_summary` en la
 * base report_db propia de este servicio. Se reconstruye unicamente a partir de los
 * eventos de Kafka que publican ticket-service/ai-service -- ver
 * config/ReportEventListener.java -- nunca desde una consulta directa a ticket_db.
 *
 * zone/category/priority/status se guardan como String plano, no como los enums
 * Zone/Category/Priority/TicketStatus de ticket-service: este servicio no valida el
 * vocabulario, solo lo refleja tal cual llega en el evento. Si ticket-service agrega
 * un valor nuevo maniana, report-service lo almacena sin romperse (fail-open), a
 * diferencia del lado transaccional donde un valor invalido si debe rechazarse.
 */
@Entity
@Table(name = "ticket_summary")
@IdClass(TicketSummaryId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSummary {

    @Id
    @Column(name = "zone", nullable = false)
    private String zone;

    @Id
    @Column(name = "ticket_id", nullable = false)
    private UUID ticketId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "technician_id")
    private UUID technicianId;

    @Column(name = "category")
    private String category;

    @Column(name = "priority")
    private String priority;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}

package ec.edu.uteq.soporte.reportservice.infrastructure.persistence;

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
 * Mapeo JPA real de TicketSummary a `ticket_summary` en la base report_db propia de
 * este servicio (ver db-cluster/scripts/init_report_db.sql).
 */
@Entity
@Table(name = "ticket_summary")
@IdClass(TicketSummaryJpaId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketSummaryJpaEntity {

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

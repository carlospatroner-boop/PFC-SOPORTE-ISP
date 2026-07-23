package ec.edu.uteq.soporte.ticketservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Entidad Ticket, mapeada a la tabla particionada `tickets` del cluster CockroachDB
 * (fragmentacion horizontal por zona -- ver ADR-0003 y db-cluster/scripts/init_db.sql).
 */
@Entity
@Table(name = "tickets")
@IdClass(TicketId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private Zone zone;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "technician_id")
    private UUID technicianId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TicketStatus status;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sla_deadline")
    private OffsetDateTime slaDeadline;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "sla_breached")
    private boolean slaBreached;
}

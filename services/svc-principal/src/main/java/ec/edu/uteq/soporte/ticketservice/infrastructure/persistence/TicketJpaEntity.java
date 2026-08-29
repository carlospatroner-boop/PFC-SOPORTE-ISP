package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Category;
import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.TicketId;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
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
 * Mapeo JPA real a la tabla particionada `tickets` de CockroachDB (fragmentacion horizontal
 * por created_at -- ver ADR-0003 y db-cluster/scripts/init_db.sql). Antes del refactor de la
 * Entrega 4, estas anotaciones vivian directamente en domain/Ticket.java; ahora esa clase es
 * un POJO de dominio puro y esta es su unica contraparte con detalles de persistencia
 * (Modulo A, item 2 de la guia de E4). La conversion entre ambas vive en TicketMapper.
 */
@Entity
@Table(name = "tickets")
@IdClass(TicketId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketJpaEntity {

    @Id
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone", nullable = false)
    private Zone zone;

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

    @Column(name = "sla_deadline")
    private OffsetDateTime slaDeadline;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "sla_breached")
    private boolean slaBreached;
}

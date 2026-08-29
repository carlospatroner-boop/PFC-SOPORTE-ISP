package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import org.springframework.stereotype.Component;

/** Traduce entre el modelo de dominio puro (Ticket) y su mapeo JPA (TicketJpaEntity). */
@Component
public class TicketMapper {

    public Ticket toDomain(TicketJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Ticket.builder()
                .createdAt(entity.getCreatedAt())
                .id(entity.getId())
                .zone(entity.getZone())
                .clientId(entity.getClientId())
                .technicianId(entity.getTechnicianId())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .slaDeadline(entity.getSlaDeadline())
                .resolvedAt(entity.getResolvedAt())
                .slaBreached(entity.isSlaBreached())
                .build();
    }

    public TicketJpaEntity toEntity(Ticket ticket) {
        if (ticket == null) {
            return null;
        }
        return TicketJpaEntity.builder()
                .createdAt(ticket.getCreatedAt())
                .id(ticket.getId())
                .zone(ticket.getZone())
                .clientId(ticket.getClientId())
                .technicianId(ticket.getTechnicianId())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .description(ticket.getDescription())
                .slaDeadline(ticket.getSlaDeadline())
                .resolvedAt(ticket.getResolvedAt())
                .slaBreached(ticket.isSlaBreached())
                .build();
    }
}

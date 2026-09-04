package ec.edu.uteq.soporte.reportservice.infrastructure.persistence;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import org.springframework.stereotype.Component;

/** Traduce entre el modelo de dominio puro (TicketSummary) y su mapeo JPA (TicketSummaryJpaEntity). */
@Component
public class TicketSummaryMapper {

    public TicketSummary toDomain(TicketSummaryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return TicketSummary.builder()
                .zone(entity.getZone())
                .ticketId(entity.getTicketId())
                .clientId(entity.getClientId())
                .technicianId(entity.getTechnicianId())
                .category(entity.getCategory())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public TicketSummaryJpaEntity toEntity(TicketSummary summary) {
        if (summary == null) {
            return null;
        }
        return TicketSummaryJpaEntity.builder()
                .zone(summary.getZone())
                .ticketId(summary.getTicketId())
                .clientId(summary.getClientId())
                .technicianId(summary.getTechnicianId())
                .category(summary.getCategory())
                .priority(summary.getPriority())
                .status(summary.getStatus())
                .description(summary.getDescription())
                .createdAt(summary.getCreatedAt())
                .updatedAt(summary.getUpdatedAt())
                .build();
    }
}

package ec.edu.uteq.soporte.ticketservice.presentation.dto;

import ec.edu.uteq.soporte.ticketservice.domain.Category;
import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketResponse(
        Zone zone,
        UUID ticketId,
        UUID clientId,
        UUID technicianId,
        Category category,
        Priority priority,
        TicketStatus status,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime slaDeadline,
        OffsetDateTime resolvedAt,
        boolean slaBreached
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getZone(), t.getId(), t.getClientId(), t.getTechnicianId(),
                t.getCategory(), t.getPriority(), t.getStatus(), t.getDescription(),
                t.getCreatedAt(), t.getSlaDeadline(), t.getResolvedAt(), t.isSlaBreached()
        );
    }
}

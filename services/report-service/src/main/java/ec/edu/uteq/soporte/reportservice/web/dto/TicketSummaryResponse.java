package ec.edu.uteq.soporte.reportservice.web.dto;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TicketSummaryResponse(
        String zone,
        UUID ticketId,
        UUID clientId,
        UUID technicianId,
        String category,
        String priority,
        String status,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TicketSummaryResponse from(TicketSummary t) {
        return new TicketSummaryResponse(
                t.getZone(), t.getTicketId(), t.getClientId(), t.getTechnicianId(),
                t.getCategory(), t.getPriority(), t.getStatus(), t.getDescription(),
                t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}

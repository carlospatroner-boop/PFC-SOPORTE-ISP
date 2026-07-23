package ec.edu.uteq.soporte.ticketservice.web.dto;

import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull TicketStatus status) {
}

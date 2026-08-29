package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.util.UUID;

public record UpdateTicketStatusCommand(UUID ticketId, TicketStatus newStatus, String role, Zone authZone) {
}

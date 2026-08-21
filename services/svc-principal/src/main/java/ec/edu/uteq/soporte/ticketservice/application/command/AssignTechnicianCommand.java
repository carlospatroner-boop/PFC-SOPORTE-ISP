package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.util.UUID;

public record AssignTechnicianCommand(UUID ticketId, UUID technicianId, String role, Zone authZone) {
}

package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.util.UUID;

/**
 * evidencePhoto/evidenceLatitude/evidenceLongitude son opcionales (Entregable 10 de la guia de
 * cierre): solo llegan del cierre en sitio desde el movil.
 */
public record UpdateTicketStatusCommand(
        UUID ticketId,
        TicketStatus newStatus,
        String role,
        Zone authZone,
        byte[] evidencePhoto,
        Double evidenceLatitude,
        Double evidenceLongitude
) {
}

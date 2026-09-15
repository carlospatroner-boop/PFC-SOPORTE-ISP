package ec.edu.uteq.soporte.ticketservice.presentation.dto;

import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

/**
 * evidencePhotoBase64/latitude/longitude son opcionales (Entregable 10 de la guia de cierre):
 * solo los manda el movil en el cierre en sitio (status = RESUELTO). El resto de transiciones
 * de estado (ASIGNADO, EN_PROGRESO, etc.) los deja en null.
 */
public record UpdateStatusRequest(
        @NotNull TicketStatus status,
        String evidencePhotoBase64,
        Double latitude,
        Double longitude
) {
}

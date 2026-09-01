package ec.edu.uteq.soporte.ticketservice.presentation.dto;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record IncidenciaResponse(
        UUID incidenciaId,
        Zone zone,
        OffsetDateTime createdAt,
        String correlMode,
        Set<UUID> ticketIds
) {
    public static IncidenciaResponse from(Incidencia i) {
        return new IncidenciaResponse(i.getId(), i.getZone(), i.getCreatedAt(), i.getCorrelMode(), i.getTicketIds());
    }
}

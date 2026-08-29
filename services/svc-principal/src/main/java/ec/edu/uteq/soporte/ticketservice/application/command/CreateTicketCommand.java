package ec.edu.uteq.soporte.ticketservice.application.command;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.util.UUID;

/**
 * "zone" todavia se recibe explicitamente; idealmente deberia inferirse de la direccion del
 * cliente, pero eso queda fuera de alcance (limitacion conocida, documentada en el informe
 * LaTeX). "clientId"/"role" ya vienen resueltos del token validado por auth-service, nunca
 * de un valor que el cliente pueda falsificar (ver TicketController).
 */
public record CreateTicketCommand(
        Zone zone,
        String title,
        String description,
        String contactPhone,
        String address,
        UUID clientId,
        String role
) {
}

package ec.edu.uteq.soporte.ticketservice.web.dto;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * NOTA: "zone" todavia se recibe explicitamente en el request -- idealmente deberia
 * inferirse de la direccion del cliente en vez de recibirse a ciegas, pero eso queda
 * fuera de alcance por ahora (limitacion conocida, documentar en el LaTeX).
 *
 * "clientId" YA NO viene en el body: TicketController lo obtiene del access token
 * validado por auth-service (ver AuthGatewayFilter), asi que siempre es el id real
 * del usuario autenticado, no un valor que el cliente pueda falsificar.
 */
public record CreateTicketRequest(
        @NotNull Zone zone,
        @NotBlank String title,
        @NotBlank String description,
        String contactPhone,
        String address
) {
}

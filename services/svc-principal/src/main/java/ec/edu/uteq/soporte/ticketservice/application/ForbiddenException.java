package ec.edu.uteq.soporte.ticketservice.application;

/**
 * El usuario esta autenticado (paso AuthGatewayFilter) pero su rol/zona no le
 * permite esta operacion sobre este ticket en particular -- ver TicketAuthorization.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

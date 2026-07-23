package ec.edu.uteq.soporte.ticketservice.service;

/**
 * El usuario esta autenticado (paso AuthGatewayFilter) pero su rol/zona no le
 * permite esta operacion sobre este ticket en particular -- ver la matriz de
 * autorizacion en TicketService.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}

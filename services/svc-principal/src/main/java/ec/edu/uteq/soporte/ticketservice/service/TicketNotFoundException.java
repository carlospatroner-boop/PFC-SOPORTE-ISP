package ec.edu.uteq.soporte.ticketservice.service;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(UUID id) {
        super("No se encontro el ticket " + id);
    }
}

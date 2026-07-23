package ec.edu.uteq.soporte.ticketservice.service;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;

import java.util.UUID;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(Zone zone, UUID id) {
        super("No se encontro el ticket " + id + " en la zona " + zone);
    }
}

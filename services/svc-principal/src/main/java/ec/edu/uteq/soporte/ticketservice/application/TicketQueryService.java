package ec.edu.uteq.soporte.ticketservice.application;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Lecturas (no mutan estado, por eso no son Command): separadas del flujo de comandos para
 * que el patron Command (application/command/) modele exclusivamente acciones, no consultas.
 */
@Service
public class TicketQueryService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TECNICO = "TECNICO";
    private static final String ROLE_CLIENTE = "CLIENTE";

    private final TicketRepository ticketRepository;
    private final TicketAuthorization authorization;

    public TicketQueryService(TicketRepository ticketRepository, TicketAuthorization authorization) {
        this.ticketRepository = ticketRepository;
        this.authorization = authorization;
    }

    public Ticket getTicket(UUID id, String role, UUID userId, Zone authZone) {
        Ticket ticket = ticketRepository.findByTicketId(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        authorization.assertCanView(ticket, role, userId, authZone);
        return ticket;
    }

    public List<Ticket> listTickets(Zone zone, TicketStatus status, String role, UUID userId, Zone authZone) {
        if (ROLE_ADMIN.equals(role)) {
            if (zone != null && status != null) {
                return ticketRepository.findByZoneAndStatus(zone, status);
            }
            if (zone != null) {
                return ticketRepository.findByZone(zone);
            }
            if (status != null) {
                // Cruza las 3 particiones -- costo mayor, documentado en el analisis comparativo.
                return ticketRepository.findByStatus(status);
            }
            return ticketRepository.findAll();
        }
        if (ROLE_TECNICO.equals(role)) {
            // El parametro "zone" de la request se ignora a proposito: un TECNICO
            // solo puede ver su propia zona, sin importar que filtro pida.
            if (authZone == null) {
                return List.of();
            }
            return status != null
                    ? ticketRepository.findByZoneAndStatus(authZone, status)
                    : ticketRepository.findByZone(authZone);
        }
        if (ROLE_CLIENTE.equals(role)) {
            return status != null
                    ? ticketRepository.findByClientIdAndStatus(userId, status)
                    : ticketRepository.findByClientId(userId);
        }
        return List.of();
    }
}

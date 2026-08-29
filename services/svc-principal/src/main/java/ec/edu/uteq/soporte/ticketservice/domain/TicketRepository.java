package ec.edu.uteq.soporte.ticketservice.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto (patron Repository) del dominio: define como se persisten y consultan los Ticket
 * sin comprometerse con JPA, Spring Data, ni SQL. La implementacion real (adaptador) vive en
 * infrastructure/persistence/TicketRepositoryAdapter.java, delegando en un
 * SpringDataTicketRepository interno -- el dominio y la capa de aplicacion solo conocen esta
 * interfaz (Modulo A, item 3 de la guia de Entrega 4: "puertos en domain, adaptadores en
 * infrastructure").
 */
public interface TicketRepository {

    Optional<Ticket> findByTicketId(UUID id);

    List<Ticket> findByZone(Zone zone);

    List<Ticket> findByZoneAndStatus(Zone zone, TicketStatus status);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByClientId(UUID clientId);

    List<Ticket> findByClientIdAndStatus(UUID clientId, TicketStatus status);

    List<Ticket> findAll();

    Ticket save(Ticket ticket);
}

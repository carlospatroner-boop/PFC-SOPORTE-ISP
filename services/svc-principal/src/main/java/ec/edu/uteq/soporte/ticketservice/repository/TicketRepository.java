package ec.edu.uteq.soporte.ticketservice.repository;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketId;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, TicketId> {

    // Consulta acotada a una sola zona: se resuelve dentro de una unica particion
    // (equivalente a la Q2 de queries_bench.sql -- ver Paso 5 / analisis comparativo).
    List<Ticket> findByZone(Zone zone);

    List<Ticket> findByZoneAndStatus(Zone zone, TicketStatus status);

    // Consulta que cruza las 3 particiones (scatter-gather) -- se documenta en el
    // documento LaTeX como el caso donde la fragmentacion aporta menos valor.
    List<Ticket> findByStatus(TicketStatus status);

    // "Mis tickets" de un CLIENTE: tambien cruza particiones (el cliente no esta
    // atado a una sola zona), misma clase de costo que findByStatus.
    List<Ticket> findByClientId(UUID clientId);

    List<Ticket> findByClientIdAndStatus(UUID clientId, TicketStatus status);
}

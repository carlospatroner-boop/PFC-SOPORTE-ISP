package ec.edu.uteq.soporte.ticketservice.repository;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketId;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, TicketId> {

    // Punto de acceso real del sistema: buscar un ticket por su id sin conocer su
    // fecha_apertura de antemano. Usa el indice unico secundario tickets_id_key
    // (ver init_db.sql) -- desde que la fragmentacion es por created_at, findById(
    // TicketId) heredado de JpaRepository exige ambos componentes de la PK, que el
    // llamador normalmente no tiene disponibles.
    @Query("select t from Ticket t where t.id = :id")
    Optional<Ticket> findByTicketId(@Param("id") UUID id);

    // Consulta acotada a una sola zona -- ya no se resuelve dentro de una unica
    // particion (la fragmentacion es por fecha, ver ADR-0003): cruza las 4
    // particiones trimestrales, filtrando por el indice secundario idx_tickets_zone.
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

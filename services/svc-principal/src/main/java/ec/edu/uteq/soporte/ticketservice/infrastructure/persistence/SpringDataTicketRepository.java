package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.TicketId;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador tecnico de Spring Data JPA -- SOLO lo usa TicketRepositoryAdapter. El resto del
 * sistema (dominio, aplicacion, presentacion) depende exclusivamente del puerto
 * domain/TicketRepository, nunca de esta interfaz directamente.
 */
interface SpringDataTicketRepository extends JpaRepository<TicketJpaEntity, TicketId> {

    @Query("select t from TicketJpaEntity t where t.id = :id")
    Optional<TicketJpaEntity> findByTicketId(@Param("id") UUID id);

    List<TicketJpaEntity> findByZone(Zone zone);

    List<TicketJpaEntity> findByZoneAndStatus(Zone zone, TicketStatus status);

    List<TicketJpaEntity> findByStatus(TicketStatus status);

    List<TicketJpaEntity> findByClientId(UUID clientId);

    List<TicketJpaEntity> findByClientIdAndStatus(UUID clientId, TicketStatus status);
}

package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador tecnico de Spring Data JPA -- SOLO lo usa IncidenciaRepositoryAdapter, mismo
 * criterio de encapsulamiento que SpringDataTicketRepository.
 */
interface SpringDataIncidenciaRepository extends JpaRepository<IncidenciaJpaEntity, UUID> {

    List<IncidenciaJpaEntity> findByZoneAndCreatedAtAfter(Zone zone, OffsetDateTime desde);

    List<IncidenciaJpaEntity> findByZone(Zone zone);
}

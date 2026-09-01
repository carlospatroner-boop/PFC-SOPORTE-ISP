package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Incidencia;
import ec.edu.uteq.soporte.ticketservice.domain.IncidenciaRepository;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Adaptador (patron Repository) que implementa el puerto de dominio IncidenciaRepository
 * delegando en Spring Data JPA -- mismo criterio exacto que TicketRepositoryAdapter.
 */
@Repository
public class IncidenciaRepositoryAdapter implements IncidenciaRepository {

    private final SpringDataIncidenciaRepository jpaRepository;
    private final IncidenciaMapper mapper;

    public IncidenciaRepositoryAdapter(SpringDataIncidenciaRepository jpaRepository, IncidenciaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Incidencia> findByZoneAndCreatedAtAfter(Zone zone, OffsetDateTime desde) {
        return jpaRepository.findByZoneAndCreatedAtAfter(zone, desde).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Incidencia> findByZone(Zone zone) {
        return jpaRepository.findByZone(zone).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Incidencia> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Incidencia save(Incidencia incidencia) {
        if (incidencia.getId() == null) {
            incidencia.setId(UUID.randomUUID());
        }
        IncidenciaJpaEntity saved = jpaRepository.save(mapper.toEntity(incidencia));
        return mapper.toDomain(saved);
    }
}

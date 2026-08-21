package ec.edu.uteq.soporte.ticketservice.infrastructure.persistence;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador (patron Repository) que implementa el puerto de dominio TicketRepository
 * delegando en Spring Data JPA. Traduce entre el modelo de dominio puro (Ticket) y la
 * entidad JPA (TicketJpaEntity) en cada operacion via TicketMapper -- este es el UNICO punto
 * del sistema donde ambos mundos se tocan.
 */
@Repository
public class TicketRepositoryAdapter implements TicketRepository {

    private final SpringDataTicketRepository jpaRepository;
    private final TicketMapper mapper;

    public TicketRepositoryAdapter(SpringDataTicketRepository jpaRepository, TicketMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Ticket> findByTicketId(UUID id) {
        return jpaRepository.findByTicketId(id).map(mapper::toDomain);
    }

    @Override
    public List<Ticket> findByZone(Zone zone) {
        return jpaRepository.findByZone(zone).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByZoneAndStatus(Zone zone, TicketStatus status) {
        return jpaRepository.findByZoneAndStatus(zone, status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByStatus(TicketStatus status) {
        return jpaRepository.findByStatus(status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByClientId(UUID clientId) {
        return jpaRepository.findByClientId(clientId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findByClientIdAndStatus(UUID clientId, TicketStatus status) {
        return jpaRepository.findByClientIdAndStatus(clientId, status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Ticket> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Ticket save(Ticket ticket) {
        TicketJpaEntity saved = jpaRepository.save(mapper.toEntity(ticket));
        return mapper.toDomain(saved);
    }
}

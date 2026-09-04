package ec.edu.uteq.soporte.reportservice.infrastructure.persistence;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummaryRepository;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Adaptador (patron Repository) que implementa el puerto de dominio
 * TicketSummaryRepository delegando en Spring Data JPA -- mismo criterio exacto que
 * TicketRepositoryAdapter en ticket-service. Tambien traduce el resultado crudo de
 * las consultas JPQL agregadas ({@code List<Object[]>}) a {@code Map<String, Long>},
 * para que ese detalle de JPA no se filtre hacia el dominio ni la aplicacion.
 */
@Repository
public class TicketSummaryRepositoryAdapter implements TicketSummaryRepository {

    private final SpringDataTicketSummaryRepository jpaRepository;
    private final TicketSummaryMapper mapper;

    public TicketSummaryRepositoryAdapter(SpringDataTicketSummaryRepository jpaRepository, TicketSummaryMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<TicketSummary> findByZoneAndTicketId(String zone, UUID ticketId) {
        return jpaRepository.findById(new TicketSummaryJpaId(zone, ticketId)).map(mapper::toDomain);
    }

    @Override
    public List<TicketSummary> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Map<String, Long> countGroupedByStatus() {
        return toMap(jpaRepository.countGroupedByStatus());
    }

    @Override
    public Map<String, Long> countGroupedByZone() {
        return toMap(jpaRepository.countGroupedByZone());
    }

    @Override
    public Map<String, Long> countGroupedByCategory() {
        return toMap(jpaRepository.countGroupedByCategory());
    }

    @Override
    public TicketSummary save(TicketSummary summary) {
        TicketSummaryJpaEntity saved = jpaRepository.save(mapper.toEntity(summary));
        return mapper.toDomain(saved);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(Objects.toString(row[0], "DESCONOCIDO"), (Long) row[1]);
        }
        return map;
    }
}

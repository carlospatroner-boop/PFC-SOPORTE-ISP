package ec.edu.uteq.soporte.reportservice.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto (patron Repository) del dominio para TicketSummary -- mismo criterio que
 * domain/TicketRepository.java en ticket-service. La implementacion real vive en
 * infrastructure/persistence/TicketSummaryRepositoryAdapter.java.
 *
 * <p>Los tres metodos "countGroupedBy*" devuelven agregados calculados del lado de
 * la base (JPQL {@code group by}) en vez de en memoria: es la razon de ser del
 * modelo de lectura CQRS, y se conserva ese comportamiento tal cual al mover la
 * consulta detras del puerto -- el adaptador traduce el resultado crudo de JPA
 * ({@code List<Object[]>}) a un {@code Map} legible por el dominio, para no filtrar
 * ese detalle de JPA hacia el resto del sistema.
 */
public interface TicketSummaryRepository {

    Optional<TicketSummary> findByZoneAndTicketId(String zone, UUID ticketId);

    List<TicketSummary> findAll();

    long count();

    Map<String, Long> countGroupedByStatus();

    Map<String, Long> countGroupedByZone();

    Map<String, Long> countGroupedByCategory();

    TicketSummary save(TicketSummary summary);
}

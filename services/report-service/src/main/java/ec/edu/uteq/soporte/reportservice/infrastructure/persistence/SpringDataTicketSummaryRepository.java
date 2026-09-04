package ec.edu.uteq.soporte.reportservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * Adaptador tecnico de Spring Data JPA -- SOLO lo usa TicketSummaryRepositoryAdapter,
 * mismo criterio de encapsulamiento que en auth-service/ticket-service. La busqueda
 * por clave compuesta usa el findById(TicketSummaryJpaId) que ya trae JpaRepository,
 * en vez de un metodo derivado propio -- evita cualquier ambiguedad de Spring Data
 * al derivar una consulta sobre campos que tambien son parte de un @IdClass.
 */
interface SpringDataTicketSummaryRepository extends JpaRepository<TicketSummaryJpaEntity, TicketSummaryJpaId> {

    // El dataset de un PFC es chico: filtrar en memoria (ver ReportQueryService) sobre
    // findAll() es mas simple que combinar findByZoneAndStatusAndCategory(...) para
    // cada subconjunto posible de los 3 filtros opcionales del endpoint /tickets.

    @Query("select t.status, count(t) from TicketSummaryJpaEntity t group by t.status")
    List<Object[]> countGroupedByStatus();

    @Query("select t.zone, count(t) from TicketSummaryJpaEntity t group by t.zone")
    List<Object[]> countGroupedByZone();

    @Query("select t.category, count(t) from TicketSummaryJpaEntity t where t.category is not null group by t.category")
    List<Object[]> countGroupedByCategory();
}

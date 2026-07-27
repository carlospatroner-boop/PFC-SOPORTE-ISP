package ec.edu.uteq.soporte.reportservice.repository;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketSummaryRepository extends JpaRepository<TicketSummary, TicketSummaryId> {

    // El dataset de un PFC es chico: filtrar en memoria (ver ReportService) sobre
    // findAll() es mas simple que combinar findByZoneAndStatusAndCategory(...) para
    // cada subconjunto posible de los 3 filtros opcionales del endpoint /tickets.

    @Query("select t.status, count(t) from TicketSummary t group by t.status")
    List<Object[]> countGroupedByStatus();

    @Query("select t.zone, count(t) from TicketSummary t group by t.zone")
    List<Object[]> countGroupedByZone();

    @Query("select t.category, count(t) from TicketSummary t where t.category is not null group by t.category")
    List<Object[]> countGroupedByCategory();
}

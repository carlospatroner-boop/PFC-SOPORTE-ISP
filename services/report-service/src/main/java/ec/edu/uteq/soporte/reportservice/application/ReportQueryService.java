package ec.edu.uteq.soporte.reportservice.application;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummaryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Orquesta el lado de consulta del CQRS -- antes vivia embebido directamente en
 * ReportController (el propio controlador llamaba a TicketSummaryRepository y
 * filtraba en memoria), lo que mezclaba la capa de presentacion con la logica de
 * consulta. Se extrae aqui, mismo criterio que TicketQueryService en ticket-service,
 * para que el controlador solo traduzca HTTP <-> DTOs.
 */
@Service
public class ReportQueryService {

    private final TicketSummaryRepository repository;

    public ReportQueryService(TicketSummaryRepository repository) {
        this.repository = repository;
    }

    public long totalCount() {
        return repository.count();
    }

    public Map<String, Long> countGroupedByStatus() {
        return repository.countGroupedByStatus();
    }

    public Map<String, Long> countGroupedByZone() {
        return repository.countGroupedByZone();
    }

    public Map<String, Long> countGroupedByCategory() {
        return repository.countGroupedByCategory();
    }

    // Dataset de un PFC, no de produccion a escala: filtrar en memoria sobre
    // findAll() es mas simple que una combinatoria de metodos derivados para los 3
    // filtros opcionales (ver comentario en el puerto TicketSummaryRepository).
    public List<TicketSummary> filtered(String zone, String status, String category) {
        return repository.findAll().stream()
                .filter(t -> zone == null || zone.equalsIgnoreCase(t.getZone()))
                .filter(t -> status == null || status.equalsIgnoreCase(t.getStatus()))
                .filter(t -> category == null || category.equalsIgnoreCase(t.getCategory()))
                .toList();
    }
}

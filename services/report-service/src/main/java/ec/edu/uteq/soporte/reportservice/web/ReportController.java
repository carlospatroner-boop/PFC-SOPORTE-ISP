package ec.edu.uteq.soporte.reportservice.web;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.repository.TicketSummaryRepository;
import ec.edu.uteq.soporte.reportservice.web.dto.ApiResponse;
import ec.edu.uteq.soporte.reportservice.web.dto.SummaryResponse;
import ec.edu.uteq.soporte.reportservice.web.dto.TicketSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lado de consulta del CQRS: todo aqui lee de ticket_summary (report_db), nunca de
 * ticket_db. Cada endpoint requiere un token ADMIN valido -- lo exige
 * config/AuthGatewayFilter.java antes de llegar hasta aca.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final TicketSummaryRepository repository;

    public ReportController(TicketSummaryRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryResponse> summary() {
        long total = repository.count();
        Map<String, Long> byStatus = toMap(repository.countGroupedByStatus());
        Map<String, Long> byZone = toMap(repository.countGroupedByZone());
        Map<String, Long> byCategory = toMap(repository.countGroupedByCategory());
        return ApiResponse.of(new SummaryResponse(total, byStatus, byZone, byCategory), "OK");
    }

    @GetMapping("/tickets")
    public ApiResponse<List<TicketSummaryResponse>> tickets(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        List<TicketSummaryResponse> result = filtered(zone, status, category).stream()
                .map(TicketSummaryResponse::from)
                .toList();
        return ApiResponse.of(result, "OK");
    }

    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        String csv = CsvExporter.toCsv(filtered(zone, status, category));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .body(csv);
    }

    // Dataset de un PFC, no de produccion a escala: filtrar en memoria sobre
    // findAll() es mas simple que una combinatoria de metodos derivados para los 3
    // filtros opcionales (ver comentario en TicketSummaryRepository).
    private List<TicketSummary> filtered(String zone, String status, String category) {
        return repository.findAll().stream()
                .filter(t -> zone == null || zone.equalsIgnoreCase(t.getZone()))
                .filter(t -> status == null || status.equalsIgnoreCase(t.getStatus()))
                .filter(t -> category == null || category.equalsIgnoreCase(t.getCategory()))
                .toList();
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(Objects.toString(row[0], "DESCONOCIDO"), (Long) row[1]);
        }
        return map;
    }
}

package ec.edu.uteq.soporte.reportservice.presentation;

import ec.edu.uteq.soporte.reportservice.application.ReportQueryService;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.presentation.dto.ApiResponse;
import ec.edu.uteq.soporte.reportservice.presentation.dto.SummaryResponse;
import ec.edu.uteq.soporte.reportservice.presentation.dto.TicketSummaryResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lado de consulta del CQRS: todo aqui lee de ticket_summary (report_db) a traves de
 * ReportQueryService, nunca de ticket_db. Cada endpoint requiere un token ADMIN
 * valido -- lo exige infrastructure/security/AuthGatewayFilter.java antes de llegar
 * hasta aca.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportQueryService queryService;

    public ReportController(ReportQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/summary")
    public ApiResponse<SummaryResponse> summary() {
        SummaryResponse response = new SummaryResponse(
                queryService.totalCount(),
                queryService.countGroupedByStatus(),
                queryService.countGroupedByZone(),
                queryService.countGroupedByCategory());
        return ApiResponse.of(response, "OK");
    }

    @GetMapping("/tickets")
    public ApiResponse<List<TicketSummaryResponse>> tickets(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        List<TicketSummaryResponse> result = queryService.filtered(zone, status, category).stream()
                .map(TicketSummaryResponse::from)
                .toList();
        return ApiResponse.of(result, "OK");
    }

    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        List<TicketSummary> filtered = queryService.filtered(zone, status, category);
        String csv = CsvExporter.toCsv(filtered);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .body(csv);
    }
}

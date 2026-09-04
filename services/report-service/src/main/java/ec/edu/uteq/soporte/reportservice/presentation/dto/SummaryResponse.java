package ec.edu.uteq.soporte.reportservice.presentation.dto;

import java.util.Map;

/**
 * Payload de GET /api/v1/reports/summary -- el "payoff" del CQRS: agregados
 * calculados sobre la tabla de lectura purpose-built ticket_summary, en vez de
 * agregar sobre la tabla transaccional de ticket-service en cada consulta.
 */
public record SummaryResponse(long totalTickets, Map<String, Long> byStatus, Map<String, Long> byZone, Map<String, Long> byCategory) {
}

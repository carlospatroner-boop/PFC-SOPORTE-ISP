package ec.edu.uteq.soporte.reportservice.presentation;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;

import java.util.List;

/**
 * Formateo de ticket_summary a CSV para GET /api/v1/reports/export.csv. Funcion
 * pura, sin dependencias de Spring -- testeable directamente (ver
 * CsvExporterTest), igual que dispatcher.js en notification-service o
 * classifier.py en ai-service.
 */
public final class CsvExporter {

    private static final String[] HEADERS = {
            "zone", "ticketId", "clientId", "technicianId", "category", "priority",
            "status", "description", "createdAt", "updatedAt"
    };

    private CsvExporter() {
    }

    public static String toCsv(List<TicketSummary> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", HEADERS)).append("\n");
        for (TicketSummary t : rows) {
            sb.append(escape(t.getZone())).append(',')
                    .append(escape(t.getTicketId())).append(',')
                    .append(escape(t.getClientId())).append(',')
                    .append(escape(t.getTechnicianId())).append(',')
                    .append(escape(t.getCategory())).append(',')
                    .append(escape(t.getPriority())).append(',')
                    .append(escape(t.getStatus())).append(',')
                    .append(escape(t.getDescription())).append(',')
                    .append(escape(t.getCreatedAt())).append(',')
                    .append(escape(t.getUpdatedAt()))
                    .append("\n");
        }
        return sb.toString();
    }

    // Envuelve en comillas y escapa comillas internas si el valor contiene coma,
    // comilla o salto de linea -- una descripcion de ticket es texto libre y puede
    // traer cualquiera de los tres.
    private static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}

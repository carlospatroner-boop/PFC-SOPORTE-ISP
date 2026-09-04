package ec.edu.uteq.soporte.reportservice.presentation;

import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @Test
    void toCsv_includesHeaderAndOneRowPerTicket() {
        UUID ticketId = UUID.randomUUID();
        TicketSummary ticket = TicketSummary.builder()
                .zone("QUEVEDO_NORTE")
                .ticketId(ticketId)
                .status("NUEVO")
                .category("CONECTIVIDAD")
                .description("Sin internet")
                .build();

        String csv = CsvExporter.toCsv(List.of(ticket));
        String[] lines = csv.split("\n");

        assertThat(lines[0]).isEqualTo("zone,ticketId,clientId,technicianId,category,priority,status,description,createdAt,updatedAt");
        assertThat(lines[1]).contains("QUEVEDO_NORTE", ticketId.toString(), "CONECTIVIDAD", "NUEVO", "Sin internet");
    }

    @Test
    void toCsv_quotesFieldsContainingCommas() {
        TicketSummary ticket = TicketSummary.builder()
                .zone("QUEVEDO_SUR")
                .ticketId(UUID.randomUUID())
                .status("NUEVO")
                .description("Sin internet, corte total")
                .build();

        String csv = CsvExporter.toCsv(List.of(ticket));

        assertThat(csv).contains("\"Sin internet, corte total\"");
    }

    @Test
    void toCsv_escapesEmbeddedQuotes() {
        TicketSummary ticket = TicketSummary.builder()
                .zone("QUEVEDO_SUR")
                .ticketId(UUID.randomUUID())
                .status("NUEVO")
                .description("El router dice \"sin señal\"")
                .build();

        String csv = CsvExporter.toCsv(List.of(ticket));

        assertThat(csv).contains("\"El router dice \"\"sin señal\"\"\"");
    }

    @Test
    void toCsv_withNoRows_returnsOnlyHeader() {
        String csv = CsvExporter.toCsv(List.of());

        assertThat(csv.strip()).isEqualTo("zone,ticketId,clientId,technicianId,category,priority,status,description,createdAt,updatedAt");
    }
}

package ec.edu.uteq.soporte.reportservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummary;
import ec.edu.uteq.soporte.reportservice.domain.TicketSummaryRepository;
import ec.edu.uteq.soporte.reportservice.domain.event.TicketAssignedEvent;
import ec.edu.uteq.soporte.reportservice.domain.event.TicketClassifiedEvent;
import ec.edu.uteq.soporte.reportservice.domain.event.TicketCreatedEvent;
import ec.edu.uteq.soporte.reportservice.domain.event.TicketStatusChangedEvent;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lado de escritura del modelo de lectura CQRS: reconstruye `ticket_summary`
 * (report_db) unicamente a partir de los 4 eventos que publican ticket-service/
 * ai-service. Vive en infrastructure/messaging (no en application) porque
 * @KafkaListener es un detalle tecnico de transporte -- mismo criterio que
 * TicketClassificationListener en ticket-service -- y depende solo del puerto de
 * dominio TicketSummaryRepository, no de Spring Data JPA directamente.
 *
 * Cada uno de los 4 @KafkaListener corre en su propio hilo/consumer group
 * independiente -- Spring no da ninguna garantia de orden *entre* topicos
 * distintos, solo dentro de una misma particion de un mismo topico. En la
 * practica esto se observa de verdad: ai-service a veces clasifica y publica
 * "ticket.classified" en milisegundos, y ese mensaje puede procesarse *antes*
 * de que el listener de "ticket.created" haya insertado la fila. Por eso cada
 * metodo aqui hace upsert (crear la fila si no existe todavia, con los campos
 * que trae ese evento y valores por defecto razonables para el resto) en vez
 * de descartar el mensaje -- los eventos convergen al estado final sin importar
 * en que orden lleguen.
 *
 * Esto expone una segunda carrera: si dos de estos hilos ven la fila ausente al
 * mismo tiempo, ambos intentan INSERT y CockroachDB (aislamiento serializable)
 * aborta una de las dos transacciones con un conflicto de escritura (visto en
 * vivo: WriteTooOldError). applyWithRetry reintenta una vez en ese caso --
 * para el segundo intento la transaccion ganadora ya confirmo, asi que
 * withSummary encuentra la fila y hace UPDATE en vez de repetir el INSERT.
 *
 * Un mensaje malformado (no un conflicto de BD) si se registra y se ignora sin
 * reintentar -- mismo patron defensivo que TicketClassificationListener en
 * ticket-service.
 */
@Component
public class ReportEventListener {

    private static final String STATUS_ASIGNADO = "ASIGNADO";
    private static final Logger LOGGER = Logger.getLogger(ReportEventListener.class.getName());

    private final TicketSummaryRepository repository;
    private final ObjectMapper objectMapper;

    public ReportEventListener(TicketSummaryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ticket.created", groupId = "report-service")
    public void onTicketCreated(String payload) {
        try {
            TicketCreatedEvent event = objectMapper.readValue(payload, TicketCreatedEvent.class);
            OffsetDateTime createdAt = event.createdAt() != null ? OffsetDateTime.parse(event.createdAt()) : OffsetDateTime.now();
            // Puede que classified/status-changed/assigned ya hayan creado una fila
            // provisional para este ticket (ver withSummary) si llegaron primero. Aqui
            // solo se completan los campos que "ticket.created" es dueno de conocer
            // (clientId/description/createdAt); el status solo se fuerza a NUEVO si la
            // fila es realmente nueva, para no revertir un progreso ya aplicado por
            // otro evento adelantado.
            applyWithRetry(event.zone(), event.ticketId(), summary -> {
                summary.setClientId(event.clientId() != null ? UUID.fromString(event.clientId()) : null);
                summary.setDescription(event.description());
                summary.setCreatedAt(createdAt);
                if (summary.getStatus() == null) {
                    summary.setStatus("NUEVO");
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar un mensaje de ticket.created: " + payload, e);
        }
    }

    @KafkaListener(topics = "ticket.classified", groupId = "report-service")
    public void onTicketClassified(String payload) {
        try {
            TicketClassifiedEvent event = objectMapper.readValue(payload, TicketClassifiedEvent.class);
            applyWithRetry(event.zone(), event.ticketId(), summary -> {
                summary.setCategory(event.category());
                summary.setPriority(event.priority());
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar un mensaje de ticket.classified: " + payload, e);
        }
    }

    @KafkaListener(topics = "ticket.status-changed", groupId = "report-service")
    public void onTicketStatusChanged(String payload) {
        try {
            TicketStatusChangedEvent event = objectMapper.readValue(payload, TicketStatusChangedEvent.class);
            applyWithRetry(event.zone(), event.ticketId(), summary -> summary.setStatus(event.newStatus()));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar un mensaje de ticket.status-changed: " + payload, e);
        }
    }

    @KafkaListener(topics = "ticket.assigned", groupId = "report-service")
    public void onTicketAssigned(String payload) {
        try {
            TicketAssignedEvent event = objectMapper.readValue(payload, TicketAssignedEvent.class);
            applyWithRetry(event.zone(), event.ticketId(), summary -> {
                summary.setTechnicianId(UUID.fromString(event.technicianId()));
                summary.setStatus(STATUS_ASIGNADO);
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar un mensaje de ticket.assigned: " + payload, e);
        }
    }

    // Aplica "mutator" sobre la fila (existente o provisional) y guarda, reintentando
    // una sola vez si el INSERT choca con otro hilo creando la misma fila
    // concurrentemente (ver comentario de la clase). Un segundo conflicto seguido
    // se deja propagar -- se registra como fallo del mensaje, no se reintenta
    // indefinidamente.
    private void applyWithRetry(String zone, String ticketId, Consumer<TicketSummary> mutator) {
        try {
            saveOnce(zone, ticketId, mutator);
        } catch (DataAccessException e) {
            LOGGER.info("Conflicto de escritura concurrente sobre " + ticketId + ", reintentando una vez");
            saveOnce(zone, ticketId, mutator);
        }
    }

    private void saveOnce(String zone, String ticketId, Consumer<TicketSummary> mutator) {
        TicketSummary summary = withSummary(zone, ticketId);
        mutator.accept(summary);
        summary.setUpdatedAt(OffsetDateTime.now());
        repository.save(summary);
    }

    // Devuelve la fila existente, o una nueva minimamente poblada si "ticket.created"
    // todavia no la inserto (ver el comentario de la clase) -- el llamador solo
    // completa los campos que trae su propio evento, dejando el resto en su default
    // provisional hasta que el evento faltante llegue y complete la fila.
    private TicketSummary withSummary(String zone, String ticketId) {
        UUID id = UUID.fromString(ticketId);
        return repository.findByZoneAndTicketId(zone, id).orElseGet(() -> {
            LOGGER.info("Evento adelantado a ticket.created para " + ticketId + " -- se crea una fila provisional");
            return TicketSummary.builder()
                    .zone(zone)
                    .ticketId(id)
                    .status("NUEVO")
                    .createdAt(OffsetDateTime.now())
                    .build();
        });
    }
}

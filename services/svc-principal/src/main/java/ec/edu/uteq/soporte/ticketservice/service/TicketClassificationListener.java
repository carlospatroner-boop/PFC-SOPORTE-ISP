package ec.edu.uteq.soporte.ticketservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.Category;
import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketId;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.event.TicketClassifiedEvent;
import ec.edu.uteq.soporte.ticketservice.repository.TicketRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Otro lado de la Saga por coreografia (ver TicketService.createTicket): ai-service
 * clasifica un ticket de forma asincrona y publica el resultado en "ticket.classified".
 * Este listener lo consume y completa category/priority, que hasta este momento
 * quedan en null (el frontend los muestra como "pendiente de clasificacion").
 *
 * De paso, recalcula el SLA con la prioridad real ya conocida -- antes de esto,
 * todo ticket recibia un SLA plano de 24h independientemente de su severidad.
 */
@Component
public class TicketClassificationListener {

    private static final Logger LOGGER = Logger.getLogger(TicketClassificationListener.class.getName());

    // Horas de SLA por prioridad real, una vez que ai-service la determina.
    private static final Map<Priority, Duration> SLA_BY_PRIORITY = Map.of(
            Priority.CRITICO, Duration.ofHours(4),
            Priority.ALTO, Duration.ofHours(12),
            Priority.MEDIO, Duration.ofHours(24),
            Priority.BAJO, Duration.ofHours(48)
    );

    private final TicketRepository ticketRepository;
    private final ObjectMapper objectMapper;

    public TicketClassificationListener(TicketRepository ticketRepository, ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "ticket.classified", groupId = "ticket-service")
    public void onTicketClassified(String payload) {
        try {
            TicketClassifiedEvent event = objectMapper.readValue(payload, TicketClassifiedEvent.class);
            apply(event);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo procesar un mensaje de ticket.classified: " + payload, e);
        }
    }

    private void apply(TicketClassifiedEvent event) {
        Zone zone = Zone.valueOf(event.zone());
        TicketId id = new TicketId(zone, UUID.fromString(event.ticketId()));
        Ticket ticket = ticketRepository.findById(id).orElse(null);
        if (ticket == null) {
            LOGGER.warning("ticket.classified referencia un ticket inexistente: " + event.ticketId());
            return;
        }

        Category category = parseEnumOrNull(Category.class, event.category());
        Priority priority = parseEnumOrNull(Priority.class, event.priority());
        if (category != null) {
            ticket.setCategory(category);
        }
        if (priority != null) {
            ticket.setPriority(priority);
            ticket.setSlaDeadline(ticket.getCreatedAt().plus(SLA_BY_PRIORITY.get(priority)));
        }
        ticketRepository.save(ticket);
    }

    private <E extends Enum<E>> E parseEnumOrNull(Class<E> enumType, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Valor desconocido '" + value + "' para " + enumType.getSimpleName());
            return null;
        }
    }
}

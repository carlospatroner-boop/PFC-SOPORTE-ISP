package ec.edu.uteq.soporte.ticketservice.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.Category;
import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.event.TicketClassifiedEvent;
import ec.edu.uteq.soporte.ticketservice.domain.policy.ClassifiedSlaPolicy;
import ec.edu.uteq.soporte.ticketservice.domain.policy.SlaPolicy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador de entrada (inbound) del otro lado de la Saga por coreografia (ver
 * application/command/CreateTicketHandler): ai-service clasifica un ticket de forma asincrona
 * y publica el resultado en "ticket.classified". Este listener lo consume y completa
 * category/priority, que hasta este momento quedan en null.
 *
 * Recalcula el SLA con la {@link ClassifiedSlaPolicy} (patron Strategy, inyectada por tipo
 * concreto ya que este es precisamente el punto que decide "ya se conoce la prioridad real,
 * usa esa politica") en vez del mapa hardcodeado que tenia antes del refactor.
 */
@Component
public class TicketClassificationListener {

    private static final Logger LOGGER = Logger.getLogger(TicketClassificationListener.class.getName());

    private final TicketRepository ticketRepository;
    private final ObjectMapper objectMapper;
    private final SlaPolicy classifiedSlaPolicy;

    public TicketClassificationListener(
            TicketRepository ticketRepository,
            ObjectMapper objectMapper,
            ClassifiedSlaPolicy classifiedSlaPolicy) {
        this.ticketRepository = ticketRepository;
        this.objectMapper = objectMapper;
        this.classifiedSlaPolicy = classifiedSlaPolicy;
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
        UUID id = UUID.fromString(event.ticketId());
        Ticket ticket = ticketRepository.findByTicketId(id).orElse(null);
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
            ticket.setSlaDeadline(ticket.getCreatedAt().plus(classifiedSlaPolicy.slaFor(priority)));
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

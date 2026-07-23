package ec.edu.uteq.soporte.ticketservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketId;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.event.TicketCreatedEvent;
import ec.edu.uteq.soporte.ticketservice.repository.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.web.dto.CreateTicketRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Matriz de autorizacion por rol (roles y permisos definidos en
 * auth-service/PermissionCatalog.java, aqui solo se hacen cumplir):
 *
 * <pre>
 * Operacion         CLIENTE                    TECNICO                      ADMIN
 * Crear             si (clientId = propio)     no (403)                     si
 * Listar            solo sus propios tickets   solo los de su zona          sin restriccion
 * Ver por id        solo si es suyo (403)      solo si es de su zona (403)  sin restriccion
 * Cambiar estado     no (403)                  solo si es de su zona (403) sin restriccion
 * Asignar tecnico   no (403)                  solo si es de su zona (403) sin restriccion
 * </pre>
 *
 * Un TECNICO sin zona reconocible (authZone == null, p.ej. una cuenta creada
 * antes de que existiera este campo) no obtiene acceso amplio -- se trata como
 * "sin zona" (fail-closed), no como "todas las zonas".
 */
@Service
public class TicketService {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_TECNICO = "TECNICO";
    private static final String ROLE_CLIENTE = "CLIENTE";
    private static final String TOPIC_TICKET_CREATED = "ticket.created";
    private static final Logger LOGGER = Logger.getLogger(TicketService.class.getName());

    private final TicketRepository ticketRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TicketService(TicketRepository ticketRepository,
                          KafkaTemplate<String, String> kafkaTemplate,
                          ObjectMapper objectMapper) {
        this.ticketRepository = ticketRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Crea un ticket en estado NUEVO, sin categoria ni prioridad todavia: en la
     * arquitectura completa del PFC (Entrega 2, Capitulo 6.2 - Saga por coreografia),
     * esos campos los completa ai-service de forma asincrona via Kafka al consumir
     * el evento ticket.created. Este microservicio, por ahora, solo persiste el
     * ticket y deja pendiente esa clasificacion (integracion futura).
     */
    public Ticket createTicket(CreateTicketRequest request, UUID clientId, String role) {
        if (ROLE_TECNICO.equals(role)) {
            throw new ForbiddenException("Los tecnicos no pueden crear tickets");
        }
        OffsetDateTime now = OffsetDateTime.now();
        Ticket ticket = Ticket.builder()
                .zone(request.zone())
                .id(UUID.randomUUID())
                .clientId(clientId)
                .status(TicketStatus.NUEVO)
                .description(buildDescription(request))
                .createdAt(now)
                // SLA por defecto de 24h hasta que ai-service asigne prioridad real
                .slaDeadline(now.plusHours(24))
                .slaBreached(false)
                .build();
        Ticket saved = ticketRepository.save(ticket);
        publishTicketCreated(saved);
        return saved;
    }

    // Se publica en el mismo hilo, luego de guardar el ticket. Un fallo de Kafka
    // (broker abajo, etc.) se registra pero NUNCA revierte ni bloquea la creacion
    // del ticket -- coherente con la postura AP para esta operacion (ADR-0004): un
    // abonado debe poder reportar una incidencia aunque la mensajeria este degradada;
    // la clasificacion por ai-service simplemente queda pendiente mas tiempo.
    private void publishTicketCreated(Ticket ticket) {
        try {
            TicketCreatedEvent event = new TicketCreatedEvent(
                    ticket.getId().toString(), ticket.getZone().name(), ticket.getDescription());
            kafkaTemplate.send(TOPIC_TICKET_CREATED, event.ticketId(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "No se pudo publicar ticket.created para " + ticket.getId(), e);
        }
    }

    private String buildDescription(CreateTicketRequest request) {
        return request.title() + " -- " + request.description();
    }

    public Ticket getTicket(Zone zone, UUID id, String role, UUID userId, Zone authZone) {
        Ticket ticket = fetchTicketOrThrow(zone, id);
        assertCanView(ticket, role, userId, authZone);
        return ticket;
    }

    public List<Ticket> listTickets(Zone zone, TicketStatus status, String role, UUID userId, Zone authZone) {
        if (ROLE_ADMIN.equals(role)) {
            if (zone != null && status != null) {
                return ticketRepository.findByZoneAndStatus(zone, status);
            }
            if (zone != null) {
                return ticketRepository.findByZone(zone);
            }
            if (status != null) {
                // Cruza las 3 particiones -- costo mayor, documentado en el analisis comparativo.
                return ticketRepository.findByStatus(status);
            }
            return ticketRepository.findAll();
        }
        if (ROLE_TECNICO.equals(role)) {
            // El parametro "zone" de la request se ignora a proposito: un TECNICO
            // solo puede ver su propia zona, sin importar que filtro pida.
            if (authZone == null) {
                return List.of();
            }
            return status != null
                    ? ticketRepository.findByZoneAndStatus(authZone, status)
                    : ticketRepository.findByZone(authZone);
        }
        if (ROLE_CLIENTE.equals(role)) {
            return status != null
                    ? ticketRepository.findByClientIdAndStatus(userId, status)
                    : ticketRepository.findByClientId(userId);
        }
        return List.of();
    }

    public Ticket updateStatus(Zone zone, UUID id, TicketStatus newStatus, String role, Zone authZone) {
        Ticket ticket = fetchTicketOrThrow(zone, id);
        assertCanManage(ticket, role, authZone);

        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESUELTO) {
            ticket.setResolvedAt(OffsetDateTime.now());
            ticket.setSlaBreached(
                    ticket.getSlaDeadline() != null
                            && ticket.getResolvedAt().isAfter(ticket.getSlaDeadline())
            );
        }
        return ticketRepository.save(ticket);
    }

    public Ticket assignTechnician(Zone zone, UUID id, UUID technicianId, String role, Zone authZone) {
        Ticket ticket = fetchTicketOrThrow(zone, id);
        assertCanManage(ticket, role, authZone);

        ticket.setTechnicianId(technicianId);
        ticket.setStatus(TicketStatus.ASIGNADO);
        return ticketRepository.save(ticket);
    }

    private Ticket fetchTicketOrThrow(Zone zone, UUID id) {
        return ticketRepository.findById(new TicketId(zone, id))
                .orElseThrow(() -> new TicketNotFoundException(zone, id));
    }

    private void assertCanView(Ticket ticket, String role, UUID userId, Zone authZone) {
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        if (ROLE_TECNICO.equals(role)) {
            if (authZone == null || ticket.getZone() != authZone) {
                throw new ForbiddenException("El ticket no pertenece a tu zona");
            }
            return;
        }
        if (ROLE_CLIENTE.equals(role) && ticket.getClientId().equals(userId)) {
            return;
        }
        throw new ForbiddenException("No tienes acceso a este ticket");
    }

    // Usado por updateStatus/assignTechnician: CLIENTE nunca puede (no tiene los
    // permisos ticket:update:status / ticket:assign), TECNICO solo dentro de su zona.
    private void assertCanManage(Ticket ticket, String role, Zone authZone) {
        if (ROLE_ADMIN.equals(role)) {
            return;
        }
        if (ROLE_TECNICO.equals(role)) {
            if (authZone == null || ticket.getZone() != authZone) {
                throw new ForbiddenException("El ticket no pertenece a tu zona");
            }
            return;
        }
        throw new ForbiddenException("No tienes permiso para modificar tickets");
    }
}

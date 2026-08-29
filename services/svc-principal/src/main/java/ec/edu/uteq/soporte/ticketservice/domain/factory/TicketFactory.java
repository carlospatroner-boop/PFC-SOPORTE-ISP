package ec.edu.uteq.soporte.ticketservice.domain.factory;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.domain.policy.SlaPolicy;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Patron Factory Method: centraliza como nace un Ticket nuevo. Antes del refactor, esta
 * logica estaba en linea dentro de TicketService.createTicket() usando Ticket.builder()
 * directamente -- cualquier invariante de creacion (estado inicial NUEVO, id generado, SLA
 * por defecto) quedaba implicito y repetible solo copiando codigo.
 *
 * Con la fabrica, "crear un ticket nuevo" es una sola operacion con nombre, reutilizable desde
 * cualquier otro punto de entrada que el sistema necesite en el futuro (alta administrativa,
 * importacion masiva, etc.) sin duplicar las reglas de creacion.
 */
@Component
public class TicketFactory {

    private final SlaPolicy defaultSlaPolicy;

    public TicketFactory(SlaPolicy defaultSlaPolicy) {
        this.defaultSlaPolicy = defaultSlaPolicy;
    }

    public Ticket crearNuevo(Zone zone, UUID clientId, String description) {
        OffsetDateTime now = OffsetDateTime.now();
        return Ticket.builder()
                .id(UUID.randomUUID())
                .zone(zone)
                .clientId(clientId)
                .status(TicketStatus.NUEVO)
                .description(description)
                .createdAt(now)
                // DefaultSlaPolicy ignora el argumento (SLA plano de 24h, todavia sin
                // prioridad real que consultar) -- null es seguro solo con esa estrategia.
                .slaDeadline(now.plus(defaultSlaPolicy.slaFor(null)))
                .slaBreached(false)
                .build();
    }
}

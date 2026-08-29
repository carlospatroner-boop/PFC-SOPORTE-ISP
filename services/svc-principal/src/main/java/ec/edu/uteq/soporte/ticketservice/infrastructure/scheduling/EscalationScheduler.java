package ec.edu.uteq.soporte.ticketservice.infrastructure.scheduling;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.TicketRepository;
import ec.edu.uteq.soporte.ticketservice.domain.TicketStatus;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationChain;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationObserver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Adaptador que activa periodicamente la cadena de escalado (patron Chain of
 * Responsibility, ver domain/escalation/). Antes del refactor de la Entrega 4 no existia
 * ningun camino que llevara un ticket al estado ESCALADO -- esta pieza es la que lo hace
 * real: cada 5 minutos revisa los tickets todavia activos y aplica la cadena.
 *
 * Tambien actua como el "sujeto" del patron Observer (ver domain/escalation/EscalationObserver):
 * no decide QUE pasa cuando un ticket se escala, solo notifica a cada observador registrado
 * como bean de Spring. Los observadores concretos (log, metrica, evento de integracion) viven
 * en infrastructure/ y se pueden agregar o quitar sin tocar esta clase.
 */
@Component
public class EscalationScheduler {

    private static final Logger LOGGER = Logger.getLogger(EscalationScheduler.class.getName());
    private static final List<TicketStatus> ESTADOS_ACTIVOS =
            List.of(TicketStatus.NUEVO, TicketStatus.ASIGNADO, TicketStatus.EN_PROGRESO);

    private final TicketRepository ticketRepository;
    private final EscalationChain escalationChain;
    private final List<EscalationObserver> observers;

    public EscalationScheduler(
            TicketRepository ticketRepository, EscalationChain escalationChain, List<EscalationObserver> observers) {
        this.ticketRepository = ticketRepository;
        this.escalationChain = escalationChain;
        this.observers = observers;
    }

    @Scheduled(fixedDelay = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void evaluarEscalados() {
        List<Ticket> activos = ESTADOS_ACTIVOS.stream()
                .flatMap(status -> Stream.of(ticketRepository.findByStatus(status)))
                .flatMap(List::stream)
                .toList();

        int escalados = 0;
        for (Ticket ticket : activos) {
            Optional<String> motivo = escalationChain.evaluate(ticket);
            if (motivo.isPresent()) {
                ticket.setStatus(TicketStatus.ESCALADO);
                Ticket saved = ticketRepository.save(ticket);
                escalados++;
                notifyObservers(saved, motivo.get());
            }
        }
        if (escalados > 0) {
            LOGGER.log(Level.INFO, "Evaluacion de escalado: {0} ticket(s) escalado(s) de {1} revisados",
                    new Object[]{escalados, activos.size()});
        }
    }

    // Un observador que falla (p.ej. Kafka abajo) no debe impedir que los demas se
    // ejecuten ni que el escalado ya persistido se pierda -- el guardado en BD ya
    // ocurrio antes de notificar.
    private void notifyObservers(Ticket ticket, String motivo) {
        for (EscalationObserver observer : observers) {
            try {
                observer.onTicketEscalated(ticket, motivo);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Observador de escalado fallo para ticket " + ticket.getId(), e);
            }
        }
    }
}

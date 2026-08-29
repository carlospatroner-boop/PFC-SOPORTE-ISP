package ec.edu.uteq.soporte.ticketservice.domain.escalation;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Ensambla la cadena a partir de los eslabones registrados como beans de Spring (ordenados
 * por @Order) y expone un unico punto de entrada. Quien use esta clase (ver
 * infrastructure/scheduling/EscalationScheduler) no necesita saber cuantos eslabones hay ni
 * en que orden se evaluan.
 */
@Component
public class EscalationChain {

    private final EscalationHandler first;

    public EscalationChain(List<EscalationHandler> handlersInOrder) {
        for (int i = 0; i < handlersInOrder.size() - 1; i++) {
            handlersInOrder.get(i).linkWith(handlersInOrder.get(i + 1));
        }
        this.first = handlersInOrder.isEmpty() ? null : handlersInOrder.get(0);
    }

    /** @return el motivo de escalado si algun eslabon de la cadena decide escalar el ticket. */
    public Optional<String> evaluate(Ticket ticket) {
        return first != null ? first.handle(ticket) : Optional.empty();
    }
}

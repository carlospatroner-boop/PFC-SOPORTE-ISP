package ec.edu.uteq.soporte.ticketservice.infrastructure.scheduling;

import ec.edu.uteq.soporte.ticketservice.domain.Ticket;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Prueba el observador de logging del escalado (uno de los tres observadores concretos del
 * patron Observer, ver ADR-0005). No hay estado que verificar por assertThat directo (el
 * efecto es un log) -- la prueba de valor real es que nunca lanza excepcion ante un ticket
 * valido, ya que EscalationScheduler notifica a los tres observadores en secuencia y uno que
 * fallara silenciosamente tumbaria la evaluacion de escalado completa.
 */
class EscalationLoggingObserverTest {

    @Test
    void onTicketEscalated_noLanzaExcepcionAnteUnTicketValido() {
        EscalationLoggingObserver observer = new EscalationLoggingObserver();
        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID())
                .zone(Zone.QUEVEDO_SUR)
                .createdAt(OffsetDateTime.now())
                .build();

        assertThatCode(() -> observer.onTicketEscalated(ticket, "SLA vencido"))
                .doesNotThrowAnyException();
    }
}

package ec.edu.uteq.soporte.ticketservice.domain.policy;

import ec.edu.uteq.soporte.ticketservice.domain.Priority;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Prueba las dos implementaciones intercambiables del Strategy (Modulo A, item 4). */
class SlaPolicyTest {

    @Test
    void defaultSlaPolicy_siempreDevuelve24HorasSinImportarLaPrioridad() {
        SlaPolicy policy = new DefaultSlaPolicy();

        assertThat(policy.slaFor(null)).isEqualTo(Duration.ofHours(24));
        assertThat(policy.slaFor(Priority.CRITICO)).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void classifiedSlaPolicy_devuelvePlazoDistintoPorPrioridad() {
        SlaPolicy policy = new ClassifiedSlaPolicy();

        assertThat(policy.slaFor(Priority.CRITICO)).isEqualTo(Duration.ofHours(4));
        assertThat(policy.slaFor(Priority.ALTO)).isEqualTo(Duration.ofHours(12));
        assertThat(policy.slaFor(Priority.MEDIO)).isEqualTo(Duration.ofHours(24));
        assertThat(policy.slaFor(Priority.BAJO)).isEqualTo(Duration.ofHours(48));
    }
}

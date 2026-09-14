package ec.edu.uteq.soporte.ticketservice.infrastructure.config;

import ec.edu.uteq.soporte.ticketservice.domain.correlation.SinCorrelacionStrategy;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.TelemetryQueryPort;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.ZonaVentanaStrategy;
import ec.edu.uteq.soporte.ticketservice.domain.correlation.ZonaVentanaTelemetriaStrategy;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationChain;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.EscalationHandler;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.SlaBreachedEscalationHandler;
import ec.edu.uteq.soporte.ticketservice.domain.escalation.StaleCriticalEscalationHandler;
import ec.edu.uteq.soporte.ticketservice.domain.factory.TicketFactory;
import ec.edu.uteq.soporte.ticketservice.domain.policy.ClassifiedSlaPolicy;
import ec.edu.uteq.soporte.ticketservice.domain.policy.DefaultSlaPolicy;
import ec.edu.uteq.soporte.ticketservice.domain.policy.SlaPolicy;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * Registra como beans de Spring las clases de domain/ que antes se anotaban a si mismas con
 * @Component/@Value/@Order (Entregable 1 de la guia de cierre: "el dominio importa el marco").
 * domain/ pasa a depender solo de Java puro; este es el unico punto de infrastructure/ que
 * conoce esas clases por su tipo concreto para poder instanciarlas.
 *
 * Los nombres de metodo importan: Spring resuelve por nombre cuando hay mas de un bean del
 * mismo tipo (SlaPolicy) y no hay un calificador explicito -- por eso "defaultSlaPolicy" aqui
 * debe llamarse exactamente asi, igual que antes lo resolvia el nombre del parametro
 * "defaultSlaPolicy" en el constructor de TicketFactory.
 */
@Configuration
public class DomainBeansConfig {

    @Bean
    public DefaultSlaPolicy defaultSlaPolicy() {
        return new DefaultSlaPolicy();
    }

    @Bean
    public ClassifiedSlaPolicy classifiedSlaPolicy() {
        return new ClassifiedSlaPolicy();
    }

    @Bean
    public TicketFactory ticketFactory(SlaPolicy defaultSlaPolicy) {
        return new TicketFactory(defaultSlaPolicy);
    }

    // CORREL: CorrelationService recibe un Map<String, CorrelationStrategy> con estos tres
    // nombres exactos como clave -- deben coincidir con los valores validos de la variable de
    // entorno CORREL (c0/c1/c2). Ver application/correlation/CorrelationService.java.
    @Bean("c0")
    public SinCorrelacionStrategy sinCorrelacionStrategy() {
        return new SinCorrelacionStrategy();
    }

    @Bean("c1")
    public ZonaVentanaStrategy zonaVentanaStrategy() {
        return new ZonaVentanaStrategy();
    }

    @Bean("c2")
    public ZonaVentanaTelemetriaStrategy zonaVentanaTelemetriaStrategy(
            TelemetryQueryPort telemetryQueryPort,
            @Value("${correlation.window-minutes:15}") long ventanaMinutos) {
        return new ZonaVentanaTelemetriaStrategy(telemetryQueryPort, ventanaMinutos);
    }

    // EscalationChain: el orden de la lista es el orden real de evaluacion (SLA vencido antes
    // que critico-sin-asignar). Antes lo fijaba @Order en cada eslabon; ahora lo fija el orden
    // de este List.of() y el @Order en cada metodo @Bean, que Spring respeta igual al inyectar
    // List<EscalationHandler>.
    @Bean
    @Order(1)
    public SlaBreachedEscalationHandler slaBreachedEscalationHandler() {
        return new SlaBreachedEscalationHandler();
    }

    @Bean
    @Order(2)
    public StaleCriticalEscalationHandler staleCriticalEscalationHandler() {
        return new StaleCriticalEscalationHandler();
    }

    @Bean
    public EscalationChain escalationChain(List<EscalationHandler> handlersInOrder) {
        return new EscalationChain(handlersInOrder);
    }
}

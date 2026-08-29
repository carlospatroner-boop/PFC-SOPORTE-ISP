package ec.edu.uteq.soporte.ticketservice.infrastructure.metrics;

import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Alimenta el histograma crdb_query_duration_seconds (ver CrdbMetrics) envolviendo cada
 * metodo del adaptador de persistencia con un cronometro -- un unico punto de
 * instrumentacion via AOP, en vez de envolver cada llamada a mano. El pointcut apunta al
 * paquete infrastructure.persistence (antes del refactor de capas apuntaba a "repository",
 * el nombre del paquete original).
 */
@Aspect
@Component
public class RepositoryTimingAspect {

    private final Timer queryDuration;

    public RepositoryTimingAspect(CrdbMetrics crdbMetrics) {
        this.queryDuration = crdbMetrics.queryDurationTimer();
    }

    @Around("execution(* ec.edu.uteq.soporte.ticketservice.infrastructure.persistence..*(..))")
    public Object timeQuery(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start();
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(queryDuration);
        }
    }
}

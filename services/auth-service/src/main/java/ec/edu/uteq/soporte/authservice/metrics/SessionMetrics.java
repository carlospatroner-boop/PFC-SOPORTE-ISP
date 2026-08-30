package ec.edu.uteq.soporte.authservice.metrics;

import ec.edu.uteq.soporte.authservice.repository.RefreshTokenRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Metrica "app_active_sessions" exigida por el Modulo F de la Guia de Entrega 4 (item 3).
 * Un Gauge de Micrometer no cachea el valor: Prometheus lo recalcula en cada scrape
 * llamando de nuevo a la consulta, asi que siempre refleja el conteo real en ese instante.
 */
@Component
public class SessionMetrics {

    private final RefreshTokenRepository refreshTokenRepository;

    public SessionMetrics(RefreshTokenRepository refreshTokenRepository, MeterRegistry registry) {
        this.refreshTokenRepository = refreshTokenRepository;
        Gauge.builder("app_active_sessions", this, SessionMetrics::countActiveSessions)
                .description("Sesiones activas (refresh tokens no revocados ni vencidos)")
                .register(registry);
    }

    @PostConstruct
    void logStartup() {
        // Fuerza una lectura al arrancar para detectar temprano un problema de conexion
        // a la base de datos, en vez de esperar al primer scrape de Prometheus.
        countActiveSessions();
    }

    private double countActiveSessions() {
        return refreshTokenRepository.countByRevokedFalseAndExpiresAtAfter(OffsetDateTime.now());
    }
}

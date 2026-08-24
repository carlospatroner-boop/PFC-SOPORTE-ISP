package ec.edu.uteq.soporte.ticketservice.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;

/**
 * Metricas HTTP genericas exigidas por el Modulo F de la Guia de Entrega 4 (item 3):
 * "http_requests_total" (etiquetado por ruta, metodo y codigo) y
 * "http_request_duration_seconds". Spring Boot ya expone algo equivalente por defecto
 * como "http_server_requests_seconds", pero la rubrica pide estos nombres exactos, asi
 * que se agregan explicitos en vez de depender del nombre por defecto de Spring.
 *
 * Se usa la ruta del patron (ej. "/api/v1/tickets/{id}"), no la URI literal con el id
 * real -- de lo contrario cada ticket distinto generaria una serie temporal nueva en
 * Prometheus (cardinalidad sin limite, un problema real de observabilidad).
 */
@Component
public class HttpMetricsFilter extends HttpFilter {

    private final MeterRegistry registry;

    public HttpMetricsFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            String route = routeOf(request);
            String method = request.getMethod();
            String status = String.valueOf(response.getStatus());

            registry.counter("http_requests_total", "route", route, "method", method, "status", status)
                    .increment();

            Timer.builder("http_request_duration_seconds")
                    .tag("route", route)
                    .tag("method", method)
                    .tag("status", status)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry)
                    .record(System.nanoTime() - start, java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }

    private String routeOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : request.getRequestURI();
    }
}

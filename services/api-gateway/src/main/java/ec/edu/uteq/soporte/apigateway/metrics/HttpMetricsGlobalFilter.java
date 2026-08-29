package ec.edu.uteq.soporte.apigateway.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;

/**
 * Equivalente reactivo de HttpMetricsFilter (los otros 3 microservicios son Spring MVC
 * clasico; el gateway es Spring Cloud Gateway sobre WebFlux, asi que un
 * jakarta.servlet.Filter normal no aplica aqui -- se usa un GlobalFilter). Mismas
 * metricas exigidas por el Modulo F: "http_requests_total" y "http_request_duration_seconds",
 * etiquetadas por ruta del PFC (el prefijo /api/v1/<servicio>, no la URI completa con ids).
 */
@Component
public class HttpMetricsGlobalFilter implements GlobalFilter, Ordered {

    private final MeterRegistry registry;

    public HttpMetricsGlobalFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long start = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            ServerHttpRequest request = exchange.getRequest();
            String route = routeOf(request);
            String method = request.getMethod() != null ? request.getMethod().name() : "UNKNOWN";
            int rawStatus = exchange.getResponse().getStatusCode() != null
                    ? exchange.getResponse().getStatusCode().value()
                    : 0;
            String status = String.valueOf(rawStatus);

            registry.counter("http_requests_total", "route", route, "method", method, "status", status)
                    .increment();

            Timer.builder("http_request_duration_seconds")
                    .tag("route", route)
                    .tag("method", method)
                    .tag("status", status)
                    .publishPercentiles(0.5, 0.95, 0.99)
                    .register(registry)
                    .record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        });
    }

    // Reduce /api/v1/tickets/<uuid> a /api/v1/tickets/{id} usando el mismo criterio de
    // particion que el resto del sistema (prefijo por microservicio, ver
    // application.yml de este gateway) -- evita cardinalidad sin limite en Prometheus.
    private String routeOf(ServerHttpRequest request) {
        String path = request.getPath().value();
        return path.replaceAll("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}", "{id}");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}

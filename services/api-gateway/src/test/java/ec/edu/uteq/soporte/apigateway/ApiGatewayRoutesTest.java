package ec.edu.uteq.soporte.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Confirma que las cinco rutas declaradas en application.yml (una por microservicio del
 * PFC) quedan registradas -- si alguien borra o renombra un servicio en el enrutamiento
 * sin querer, esta prueba falla en vez de descubrirse recien en produccion.
 */
@SpringBootTest
class ApiGatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void registraUnaRutaPorCadaMicroservicioDelPfc() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).isNotNull();

        Set<String> ids = routes.stream().map(Route::getId).collect(Collectors.toSet());
        assertThat(ids).containsExactlyInAnyOrder(
                "auth-service", "ticket-service", "notification-service", "ai-service", "report-service");
    }
}

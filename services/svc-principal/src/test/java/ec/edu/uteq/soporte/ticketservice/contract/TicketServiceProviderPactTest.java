package ec.edu.uteq.soporte.ticketservice.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.HttpRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * Verificacion del lado PROVEEDOR del contrato Pact (Modulo D de la Guia de Entrega 4).
 *
 * <p>El consumidor es apps/web (ver apps/web/tests/contract/ticketsApi.pact.test.ts), que ya
 * genero el archivo de contrato en pacts/soporte-web-ticket-service.json. Esta clase repite
 * cada interaccion grabada ahi contra un ticket-service REAL (no un contexto Spring de
 * prueba), porque AuthGatewayFilter valida el JWT con una llamada HTTP real a auth-service
 * (GET /validate) -- ver infrastructure/security/AuthGatewayFilter.java -- asi que un
 * contexto de prueba aislado no podria autenticar ninguna peticion.
 *
 * <p>Por eso esta prueba NO corre en un "mvn test" normal (quedaria roja apenas nadie tenga
 * el stack levantado, por ejemplo en una maquina que solo clono el repo). Se activa a
 * proposito con una system property:
 *
 * <pre>
 *   docker compose up -d
 *   mvn test -Dtest=TicketServiceProviderPactTest -DRUN_CONTRACT_VERIFICATION=true
 * </pre>
 */
@Provider("ticket-service")
@PactFolder("../../pacts")
@EnabledIfSystemProperty(named = "RUN_CONTRACT_VERIFICATION", matches = "true")
class TicketServiceProviderPactTest {

    private static final String AUTH_SERVICE_LOGIN_URL = "http://localhost:8001/api/v1/auth/login";
    private static final String TICKET_SERVICE_HOST = "localhost";
    private static final int TICKET_SERVICE_PORT = 8002;

    private static String adminAccessToken;

    @BeforeAll
    static void obtenerTokenAdminReal() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(Map.of(
                "email", "admin@soporte.local",
                "password", "Admin123!"));

        HttpClient client = HttpClient.newHttpClient();
        java.net.http.HttpRequest loginRequest = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create(AUTH_SERVICE_LOGIN_URL))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "No se pudo autenticar contra auth-service real (" + AUTH_SERVICE_LOGIN_URL + "), status "
                            + response.statusCode() + ". Corra 'docker compose up -d' antes de esta verificacion.");
        }
        JsonNode data = mapper.readTree(response.body()).get("data");
        adminAccessToken = data.get("accessToken").asText();
    }

    @BeforeEach
    void configurarTarget(PactVerificationContext context) {
        context.setTarget(new HttpTestTarget(TICKET_SERVICE_HOST, TICKET_SERVICE_PORT, "/"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verificarInteraccion(PactVerificationContext context, HttpRequest request) {
        // El pact grabado por el consumidor trae un header Authorization de ejemplo
        // ("Bearer token-valido") que AuthGatewayFilter rechazaria (no es un JWT real).
        // Se reemplaza ese valor por el token de ADMIN real obtenido en @BeforeAll. La
        // interaccion del caso 401 (sin token) NO trae ese header, asi que queda intacta
        // a proposito: ese caso debe seguir devolviendo 401 tal como espera el contrato.
        if (request.containsHeader("Authorization")) {
            request.removeHeaders("Authorization");
            request.addHeader("Authorization", "Bearer " + adminAccessToken);
        }
        context.verifyInteraction();
    }

    @State("el rol ADMIN tiene al menos un ticket visible")
    void elRolAdminTieneAlMenosUnTicketVisible() {
        // No-op a proposito: esta verificacion corre contra el stack real ya levantado
        // (docker compose up), que ya tiene datos de demo con tickets visibles para ADMIN
        // (ver resultados/rebalance_demo_tickets.sql), asi que la precondicion ya se cumple.
    }

    @State("no importa el estado")
    void noImportaElEstado() {
        // No-op: el caso 401 sin token no depende de ningun dato existente.
    }
}

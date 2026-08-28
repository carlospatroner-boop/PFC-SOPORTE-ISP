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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

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
    void elRolAdminTieneAlMenosUnTicketVisible() throws Exception {
        // Antes esto era un no-op que asumia datos de demo ya cargados via
        // "resultados/rebalance_demo_tickets.sql" -- un script que un companero corrio una
        // vez a mano en su Docker local y nunca se llego a versionar en git (confirmado:
        // no existe en el repo ni en el historial). Por eso esta interaccion NUNCA pudo
        // pasar en una maquina/CI realmente limpios: el estado del que dependia solo
        // vivia en un volumen de Docker de un desarrollador. El @State de Pact existe
        // justo para esto -- se inserta aqui mismo, por SQL directo, exactamente el
        // ticket resuelto y el ticket nuevo que pide pacts/soporte-web-ticket-service.json,
        // asi la precondicion la garantiza la prueba y no un dato externo invisible.
        try (Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://localhost:26257/ticket_db?sslmode=disable", "root", "")) {
            try (PreparedStatement technician = conn.prepareStatement(
                    "UPSERT INTO technicians (id, full_name, zone, specialty, active) VALUES (?, ?, ?, ?, true)")) {
                technician.setObject(1, UUID.fromString("9510f44d-e785-4091-9454-6cf3e546a0cb"));
                technician.setString(2, "Tecnico Demo Pact");
                technician.setString(3, "QUEVEDO_NORTE");
                technician.setString(4, "CONECTIVIDAD");
                technician.executeUpdate();
            }

            // No se usa UPSERT aqui: UPSERT solo resuelve conflictos contra la PRIMARY KEY
            // (created_at, id), pero "id" tiene ADEMAS un indice unico propio,
            // "tickets_id_key" (ver db-cluster/scripts/init_db.sql), que UPSERT no
            // considera. Si el ticket ya existe (por ejemplo, quedo de una corrida
            // anterior de esta misma prueba, o de la vieja data de demo semi-perdida que
            // origino este arreglo), un UPSERT con un valor de created_at distinto
            // violaria ese indice en vez de actualizar la fila. DELETE + INSERT es
            // idempotente de verdad sin importar el estado previo de la tabla.
            String deleteTicket = "DELETE FROM tickets WHERE id = ?";
            String insertTicket = "INSERT INTO tickets (created_at, id, zone, client_id, technician_id, "
                    + "category, priority, status, description, sla_deadline, resolved_at, sla_breached) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement delete = conn.prepareStatement(deleteTicket)) {
                delete.setObject(1, UUID.fromString("c8e57689-c021-40df-86d3-01da9615f56c"));
                delete.executeUpdate();
            }
            try (PreparedStatement delete = conn.prepareStatement(deleteTicket)) {
                delete.setObject(1, UUID.fromString("a1e57689-c021-40df-86d3-01da9615f000"));
                delete.executeUpdate();
            }

            try (PreparedStatement resuelto = conn.prepareStatement(insertTicket)) {
                resuelto.setObject(1, OffsetDateTime.parse("2026-08-20T12:00:00Z"));
                resuelto.setObject(2, UUID.fromString("c8e57689-c021-40df-86d3-01da9615f56c"));
                resuelto.setString(3, "QUEVEDO_NORTE");
                resuelto.setObject(4, UUID.fromString("648954f1-c33c-4e88-8492-62ec39f90f0f"));
                resuelto.setObject(5, UUID.fromString("9510f44d-e785-4091-9454-6cf3e546a0cb"));
                resuelto.setString(6, "CONECTIVIDAD");
                resuelto.setString(7, "MEDIO");
                resuelto.setString(8, "RESUELTO");
                resuelto.setString(9, "Sin acceso a Internet");
                resuelto.setObject(10, OffsetDateTime.parse("2026-08-27T12:00:00Z"));
                resuelto.setObject(11, OffsetDateTime.parse("2026-08-21T09:00:00Z"));
                resuelto.setBoolean(12, false);
                resuelto.executeUpdate();
            }

            try (PreparedStatement nuevo = conn.prepareStatement(insertTicket)) {
                nuevo.setObject(1, OffsetDateTime.parse("2026-08-20T12:00:00Z"));
                nuevo.setObject(2, UUID.fromString("a1e57689-c021-40df-86d3-01da9615f000"));
                nuevo.setString(3, "QUEVEDO_NORTE");
                nuevo.setObject(4, UUID.fromString("648954f1-c33c-4e88-8492-62ec39f90f0f"));
                nuevo.setNull(5, Types.OTHER);
                nuevo.setString(6, "CONECTIVIDAD");
                nuevo.setString(7, "MEDIO");
                nuevo.setString(8, "NUEVO");
                nuevo.setString(9, "Sin acceso a Internet");
                nuevo.setObject(10, OffsetDateTime.parse("2026-08-27T12:00:00Z"));
                nuevo.setNull(11, Types.TIMESTAMP_WITH_TIMEZONE);
                nuevo.setBoolean(12, false);
                nuevo.executeUpdate();
            }
        }
    }

    @State("no importa el estado")
    void noImportaElEstado() {
        // No-op: el caso 401 sin token no depende de ningun dato existente.
    }
}

package ec.edu.uteq.soporte.ticketservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.ticketservice.domain.Zone;
import ec.edu.uteq.soporte.ticketservice.web.dto.ApiResponse;
import ec.edu.uteq.soporte.ticketservice.web.dto.ValidateResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Exige un access token valido (emitido por auth-service) en cada llamada a
 * /api/v1/tickets/**. En vez de verificar la firma del JWT aqui mismo, delega la
 * validacion a auth-service llamando a su endpoint GET /validate -- asi el secreto
 * de firma nunca sale de auth-service y la logica de validacion vive en un solo
 * lugar. Trade-off aceptado: ticket-service ahora depende en tiempo real de que
 * auth-service este arriba.
 */
@Component
public class AuthGatewayFilter extends OncePerRequestFilter {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String validateUrl;

    public AuthGatewayFilter(ObjectMapper objectMapper,
                              @Value("${auth.service.base-url}") String authServiceBaseUrl) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
        this.validateUrl = authServiceBaseUrl + "/validate";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getRequestURI().startsWith("/api/v1/tickets");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeUnauthorized(response, "Se requiere iniciar sesion (encabezado Authorization ausente)");
            return;
        }

        try {
            ApiResponse<ValidateResponse> validated = restClient.get()
                    .uri(validateUrl)
                    .header("Authorization", header)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<ValidateResponse>>() {
                    });

            if (validated == null || validated.data() == null) {
                writeUnauthorized(response, "Token invalido");
                return;
            }

            request.setAttribute("authUserId", UUID.fromString(validated.data().userId()));
            request.setAttribute("authRole", validated.data().role());
            request.setAttribute("authZone", parseZone(validated.data().zone()));
            filterChain.doFilter(request, response);
        } catch (RestClientException e) {
            writeUnauthorized(response, "Token invalido o auth-service no disponible: " + e.getMessage());
        }
    }

    // Valor invalido/desconocido se trata como "sin zona" (fail-closed): un TECNICO
    // sin zona reconocible no obtiene acceso amplio, ver TicketService.
    private Zone parseZone(String zoneClaim) {
        if (zoneClaim == null || zoneClaim.isBlank()) {
            return null;
        }
        try {
            return Zone.valueOf(zoneClaim);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), ApiResponse.of(null, message));
    }
}

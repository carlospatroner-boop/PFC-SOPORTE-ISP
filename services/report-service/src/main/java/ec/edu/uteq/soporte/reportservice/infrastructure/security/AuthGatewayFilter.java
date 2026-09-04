package ec.edu.uteq.soporte.reportservice.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uteq.soporte.reportservice.presentation.dto.ApiResponse;
import ec.edu.uteq.soporte.reportservice.presentation.dto.ValidateResponse;
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

/**
 * Exige un access token valido de un usuario ADMIN en cada llamada a
 * /api/v1/reports/**. Mismo enfoque que ticket-service (delegar la verificacion del
 * JWT a auth-service via GET /validate en vez de reimplementarla aqui), mas una
 * restriccion adicional: reportar metricas es una funcion de gestion, asi que
 * cualquier rol distinto de ADMIN recibe 403 aunque el token sea valido.
 */
@Component
public class AuthGatewayFilter extends OncePerRequestFilter {

    private static final String ROLE_ADMIN = "ADMIN";

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
                || !request.getRequestURI().startsWith("/api/v1/reports");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Se requiere iniciar sesion (encabezado Authorization ausente)");
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
                writeError(response, HttpStatus.UNAUTHORIZED, "Token invalido");
                return;
            }

            if (!ROLE_ADMIN.equals(validated.data().role())) {
                writeError(response, HttpStatus.FORBIDDEN, "Este recurso requiere el rol ADMIN");
                return;
            }

            filterChain.doFilter(request, response);
        } catch (RestClientException e) {
            writeError(response, HttpStatus.UNAUTHORIZED, "Token invalido o auth-service no disponible: " + e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), ApiResponse.of(null, message));
    }
}

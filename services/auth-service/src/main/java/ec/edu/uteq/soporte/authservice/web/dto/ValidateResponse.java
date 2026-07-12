package ec.edu.uteq.soporte.authservice.web.dto;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Respuesta del endpoint de validacion consumido por el (futuro) API Gateway.
 */
public record ValidateResponse(
        String userId,
        String email,
        String role,
        String zone,
        List<String> permissions,
        OffsetDateTime expiresAt) {
}

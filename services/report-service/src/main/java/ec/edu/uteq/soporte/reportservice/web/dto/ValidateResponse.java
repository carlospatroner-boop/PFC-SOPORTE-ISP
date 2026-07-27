package ec.edu.uteq.soporte.reportservice.web.dto;

/**
 * Copia local del subconjunto de campos que necesitamos de la respuesta de
 * auth-service (GET /api/v1/auth/validate).
 */
public record ValidateResponse(String userId, String email, String role, String zone) {
}

package ec.edu.uteq.soporte.ticketservice.web.dto;

/**
 * Copia local del subconjunto de campos que necesitamos de la respuesta de
 * auth-service (GET /api/v1/auth/validate). Cada servicio mantiene su propia copia
 * del DTO, igual que ApiResponse -- no hay modulo compartido en este repo.
 */
public record ValidateResponse(String userId, String email, String role, String zone) {
}

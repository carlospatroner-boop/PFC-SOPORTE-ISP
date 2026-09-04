package ec.edu.uteq.soporte.reportservice.presentation.dto;

import java.time.OffsetDateTime;

/**
 * Envoltura estandar de respuesta (ver ticket-service/auth-service): {"data": ...,
 * "message": ..., "timestamp": ...}. Copia local, sin modulo compartido en este repo.
 */
public record ApiResponse<T>(T data, String message, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message, OffsetDateTime.now());
    }
}

package ec.edu.uteq.soporte.authservice.web.dto;

import java.time.OffsetDateTime;

/**
 * Envoltura estandar de respuesta, igual a la usada en ticket-service:
 * {"data": ..., "message": ..., "timestamp": ...}.
 */
public record ApiResponse<T>(T data, String message, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message, OffsetDateTime.now());
    }
}

package ec.edu.uteq.soporte.ticketservice.web.dto;

import java.time.OffsetDateTime;

/**
 * Envoltura estandar de respuesta definida en el contrato REST de la Entrega 2
 * (Capitulo 7.1): {"data": ..., "message": ..., "timestamp": ...}.
 */
public record ApiResponse<T>(T data, String message, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message, OffsetDateTime.now());
    }
}

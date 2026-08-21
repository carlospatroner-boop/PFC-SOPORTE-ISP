package ec.edu.uteq.soporte.ticketservice.presentation.dto;

import java.time.OffsetDateTime;

/**
 * Envoltura estandar de respuesta: {"data": ..., "message": ..., "timestamp": ...}.
 * Tambien la usa infrastructure/security/AuthGatewayFilter para su propia respuesta de error
 * -- es un tipo de envoltorio de transporte sin logica de negocio, no una violacion real de
 * capas (se documenta como simplificacion deliberada en el ADR-005).
 */
public record ApiResponse<T>(T data, String message, OffsetDateTime timestamp) {

    public static <T> ApiResponse<T> of(T data, String message) {
        return new ApiResponse<>(data, message, OffsetDateTime.now());
    }
}

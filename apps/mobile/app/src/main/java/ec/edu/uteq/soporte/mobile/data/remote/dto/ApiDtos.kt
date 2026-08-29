package ec.edu.uteq.soporte.mobile.data.remote.dto

/**
 * Envoltura estandar de respuesta del backend: {"data": ..., "message": ..., "timestamp": ...}
 * -- identica a ApiResponse<T> en auth-service y ticket-service (ver
 * services/auth-service/.../web/dto/ApiResponse.java).
 */
data class ApiResponse<T>(
    val data: T?,
    val message: String?,
    val timestamp: String?,
)

// ---------------------------------------------------------------- auth-service (puerto 8001)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String?,
)

// ---------------------------------------------------------------- ticket-service (puerto 8002)

enum class TicketStatus {
    NUEVO, ASIGNADO, EN_PROGRESO, ESCALADO, RESUELTO, CERRADO
}

enum class Zone {
    QUEVEDO_CENTRO, QUEVEDO_NORTE, QUEVEDO_SUR
}

enum class Category {
    CONECTIVIDAD, DNS, HARDWARE, CONFIGURACION, VELOCIDAD
}

enum class Priority {
    CRITICO, ALTO, MEDIO, BAJO
}

/** Coincide campo a campo con TicketResponse.java del backend real. */
data class TicketResponse(
    val zone: Zone,
    val ticketId: String,
    val clientId: String,
    val technicianId: String?,
    val category: Category?,
    val priority: Priority?,
    val status: TicketStatus,
    val description: String,
    val createdAt: String,
    val slaDeadline: String?,
    val slaBreached: Boolean,
)

data class UpdateStatusRequest(
    val status: TicketStatus,
)

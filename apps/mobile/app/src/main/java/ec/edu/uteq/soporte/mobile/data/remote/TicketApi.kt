package ec.edu.uteq.soporte.mobile.data.remote

import ec.edu.uteq.soporte.mobile.data.remote.dto.ApiResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.UpdateStatusRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Contrato real de ticket-service (puerto 8002) -- ver TicketController.java. El filtro por
 * zona/rol lo resuelve el propio backend a partir del JWT (authRole/authZone), el cliente movil
 * no necesita mandar esos datos: solo el header Authorization (ver NetworkModule).
 */
interface TicketApi {
    // Sin valores por defecto a proposito: Retrofit invoca la interfaz via un proxy dinamico,
    // y los parametros por defecto de Kotlin en metodos de interfaz no se resuelven de forma
    // fiable a traves de ese proxy. El llamador (TicketRepository) siempre pasa null explicito
    // cuando no quiere filtrar.
    @GET("api/v1/tickets")
    suspend fun listTickets(
        @Query("zone") zone: String?,
        @Query("status") status: String?,
    ): ApiResponse<List<TicketResponse>>

    @GET("api/v1/tickets/{id}")
    suspend fun getTicket(@Path("id") id: String): ApiResponse<TicketResponse>

    @PATCH("api/v1/tickets/{id}/status")
    suspend fun updateStatus(
        @Path("id") id: String,
        @Body request: UpdateStatusRequest,
    ): ApiResponse<TicketResponse>
}

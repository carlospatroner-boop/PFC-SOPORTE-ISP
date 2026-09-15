package ec.edu.uteq.soporte.mobile.data.repository

import android.util.Base64
import ec.edu.uteq.soporte.mobile.data.local.TicketDao
import ec.edu.uteq.soporte.mobile.data.local.TicketEntity
import ec.edu.uteq.soporte.mobile.data.remote.TicketApi
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus
import ec.edu.uteq.soporte.mobile.data.remote.dto.UpdateStatusRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repositorio unico para la feature de tickets (un repositorio por caracteristica, como pide
 * el Modulo C item 2). Estrategia offline-first: la UI siempre lee de Room; refreshTickets()
 * trae del backend y actualiza la cache, para que el listado siga siendo utilizable sin red.
 */
class TicketRepository(
    private val ticketApi: TicketApi,
    private val ticketDao: TicketDao,
) {
    fun observeTickets(): Flow<List<TicketResponse>> =
        ticketDao.observeAll().map { entities -> entities.map { it.toResponse() } }

    suspend fun refreshTickets(): Result<Unit> = runCatching {
        val response = ticketApi.listTickets(zone = null, status = null)
        val tickets = response.data.orEmpty()
        val now = System.currentTimeMillis()
        ticketDao.upsertAll(tickets.map { TicketEntity.fromResponse(it, now) })
    }

    /** Cache primero (funciona sin red); si hay red intenta traer el dato mas fresco. */
    suspend fun getTicket(ticketId: String): TicketResponse? {
        val fresh = runCatching { ticketApi.getTicket(ticketId).data }.getOrNull()
        if (fresh != null) {
            ticketDao.upsert(TicketEntity.fromResponse(fresh, System.currentTimeMillis()))
            return fresh
        }
        return ticketDao.findById(ticketId)?.toResponse()
    }

    /**
     * Cierre en sitio (Modulo C, dominio ACC; Entregable 10 de la guia de cierre): manda la
     * foto de evidencia (Base64) y las coordenadas GPS capturadas en TicketDetailViewModel
     * junto con el cambio de estado a RESUELTO, en la misma llamada al endpoint que ya existia
     * -- el backend las guarda solo cuando el estado nuevo es RESUELTO (ver
     * UpdateTicketStatusHandler.java).
     *
     * Con reintento: una foto+GPS en el sitio de un tecnico de campo es tipicamente la peor
     * cobertura de red del recorrido (Modulo C, dominio ACC), asi que un solo intento fallido
     * por una caida momentanea de la conexion no deberia obligar a repetir la captura de
     * evidencia -- solo se reintenta ante un fallo de RED (IOException: sin conexion, timeout),
     * nunca ante una respuesta HTTP de error real (4xx/5xx), que indica un problema del lado
     * del servidor o de autorizacion que un reintento inmediato no va a resolver.
     */
    suspend fun closeOnSite(
        ticketId: String,
        evidencePhoto: ByteArray,
        latitude: Double,
        longitude: Double,
    ): Result<TicketResponse> = runCatching {
        val request = UpdateStatusRequest(
            status = TicketStatus.RESUELTO,
            evidencePhotoBase64 = Base64.encodeToString(evidencePhoto, Base64.NO_WRAP),
            latitude = latitude,
            longitude = longitude,
        )
        val response = updateStatusWithRetry(ticketId, request)
        val updated = requireNotNull(response.data) { "Respuesta de cierre sin datos" }
        ticketDao.upsert(TicketEntity.fromResponse(updated, System.currentTimeMillis()))
        updated
    }

    private suspend fun updateStatusWithRetry(
        ticketId: String,
        request: UpdateStatusRequest,
        maxAttempts: Int = 3,
    ) = run {
        var lastNetworkError: IOException? = null
        for (attempt in 1..maxAttempts) {
            try {
                return@run ticketApi.updateStatus(ticketId, request)
            } catch (e: IOException) {
                lastNetworkError = e
                if (attempt < maxAttempts) delay(attempt * 1_000L)
            }
        }
        throw lastNetworkError!!
    }
}

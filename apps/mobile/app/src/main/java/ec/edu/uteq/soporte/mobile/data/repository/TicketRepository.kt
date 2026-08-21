package ec.edu.uteq.soporte.mobile.data.repository

import ec.edu.uteq.soporte.mobile.data.local.TicketDao
import ec.edu.uteq.soporte.mobile.data.local.TicketEntity
import ec.edu.uteq.soporte.mobile.data.remote.TicketApi
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus
import ec.edu.uteq.soporte.mobile.data.remote.dto.UpdateStatusRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
     * Cierre en sitio (Modulo C, dominio ACC): hoy solo actualiza el estado a RESUELTO via el
     * endpoint que ya existe. La foto de evidencia y la geolocalizacion capturadas en
     * TicketDetailViewModel quedan listas para enviarse en cuanto el backend exponga el campo
     * correspondiente (pendiente de coordinar con el modulo de backend en capas).
     */
    suspend fun closeOnSite(ticketId: String): Result<TicketResponse> = runCatching {
        val response = ticketApi.updateStatus(ticketId, UpdateStatusRequest(TicketStatus.RESUELTO))
        val updated = requireNotNull(response.data) { "Respuesta de cierre sin datos" }
        ticketDao.upsert(TicketEntity.fromResponse(updated, System.currentTimeMillis()))
        updated
    }
}

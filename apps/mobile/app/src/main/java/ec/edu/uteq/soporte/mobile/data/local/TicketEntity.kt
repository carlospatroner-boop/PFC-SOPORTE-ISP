package ec.edu.uteq.soporte.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse

/** Cache local de los tickets asignados al tecnico -- ver Modulo C, item 4: "modo sin conexion". */
@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val ticketId: String,
    val zone: String,
    val clientId: String,
    val technicianId: String?,
    val category: String?,
    val priority: String?,
    val status: String,
    val description: String,
    val createdAt: String,
    val slaDeadline: String?,
    val slaBreached: Boolean,
    val cachedAtMillis: Long,
) {
    fun toResponse(): TicketResponse = TicketResponse(
        zone = ec.edu.uteq.soporte.mobile.data.remote.dto.Zone.valueOf(zone),
        ticketId = ticketId,
        clientId = clientId,
        technicianId = technicianId,
        category = category?.let { ec.edu.uteq.soporte.mobile.data.remote.dto.Category.valueOf(it) },
        priority = priority?.let { ec.edu.uteq.soporte.mobile.data.remote.dto.Priority.valueOf(it) },
        status = ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus.valueOf(status),
        description = description,
        createdAt = createdAt,
        slaDeadline = slaDeadline,
        slaBreached = slaBreached,
    )

    companion object {
        fun fromResponse(response: TicketResponse, cachedAtMillis: Long): TicketEntity = TicketEntity(
            ticketId = response.ticketId,
            zone = response.zone.name,
            clientId = response.clientId,
            technicianId = response.technicianId,
            category = response.category?.name,
            priority = response.priority?.name,
            status = response.status.name,
            description = response.description,
            createdAt = response.createdAt,
            slaDeadline = response.slaDeadline,
            slaBreached = response.slaBreached,
            cachedAtMillis = cachedAtMillis,
        )
    }
}

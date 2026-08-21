package ec.edu.uteq.soporte.mobile.ui.theme

import androidx.compose.ui.graphics.Color
import ec.edu.uteq.soporte.mobile.data.remote.dto.Category
import ec.edu.uteq.soporte.mobile.data.remote.dto.Priority
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus

/**
 * Paleta semantica de estados -- la misma navy/teal/ambar definida para las diapositivas de la
 * Entrega 3 (docs/diapositivas/build_deck.py), para que el color signifique lo mismo en todo el
 * proyecto (mazo, informe y ahora tambien la app).
 */
val StatusNuevo = Color(0xFF1E3A5F) // navy
val StatusAsignado = Color(0xFF8A5A00) // ambar
val StatusEnProgreso = Color(0xFF0F6B5C) // teal
val StatusEscalado = Color(0xFFB3261E) // rojo
val StatusResuelto = Color(0xFF2E7D32) // verde
val StatusCerrado = Color(0xFF616161) // gris

fun colorForStatus(status: TicketStatus): Color = when (status) {
    TicketStatus.NUEVO -> StatusNuevo
    TicketStatus.ASIGNADO -> StatusAsignado
    TicketStatus.EN_PROGRESO -> StatusEnProgreso
    TicketStatus.ESCALADO -> StatusEscalado
    TicketStatus.RESUELTO -> StatusResuelto
    TicketStatus.CERRADO -> StatusCerrado
}

fun labelForStatus(status: TicketStatus): String = when (status) {
    TicketStatus.NUEVO -> "Nuevo"
    TicketStatus.ASIGNADO -> "Asignado"
    TicketStatus.EN_PROGRESO -> "En progreso"
    TicketStatus.ESCALADO -> "Escalado"
    TicketStatus.RESUELTO -> "Resuelto"
    TicketStatus.CERRADO -> "Cerrado"
}

/** Prioridad: un solo tono de acento (ambar) con intensidad/etiqueta creciente -- se distingue
 * del chip de estado por forma (contorno, no relleno) para no competir visualmente con el. */
fun colorForPriority(priority: Priority): Color = when (priority) {
    Priority.CRITICO -> StatusEscalado
    Priority.ALTO -> StatusAsignado
    Priority.MEDIO -> StatusEnProgreso
    Priority.BAJO -> StatusCerrado
}

fun labelForPriority(priority: Priority): String = when (priority) {
    Priority.CRITICO -> "Crítico"
    Priority.ALTO -> "Alto"
    Priority.MEDIO -> "Medio"
    Priority.BAJO -> "Bajo"
}

fun labelForCategory(category: Category): String = when (category) {
    Category.CONECTIVIDAD -> "Conectividad"
    Category.DNS -> "DNS"
    Category.HARDWARE -> "Hardware"
    Category.CONFIGURACION -> "Configuración"
    Category.VELOCIDAD -> "Velocidad"
}

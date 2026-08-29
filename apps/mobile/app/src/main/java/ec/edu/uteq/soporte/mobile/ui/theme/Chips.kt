package ec.edu.uteq.soporte.mobile.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ec.edu.uteq.soporte.mobile.data.remote.dto.Priority
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus

/** Chip relleno -- para el estado, el dato mas importante de un vistazo. */
@Composable
fun StatusChip(status: TicketStatus, modifier: Modifier = Modifier) {
    val color = colorForStatus(status)
    Text(
        text = labelForStatus(status),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Chip con contorno -- la prioridad acompaña al estado sin competir visualmente con el. */
@Composable
fun PriorityChip(priority: Priority, modifier: Modifier = Modifier) {
    val color = colorForPriority(priority)
    Text(
        text = labelForPriority(priority),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Etiqueta de alto contraste para SLA vencido -- debe notarse antes que cualquier otro dato. */
@Composable
fun SlaBreachedChip(modifier: Modifier = Modifier) {
    Text(
        text = "⚠ SLA VENCIDO",
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(StatusEscalado)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

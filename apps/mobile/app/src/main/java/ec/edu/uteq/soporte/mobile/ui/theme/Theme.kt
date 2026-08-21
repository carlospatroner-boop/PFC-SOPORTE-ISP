package ec.edu.uteq.soporte.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF1E3A5F)
private val Teal = Color(0xFF0F6B5C)
private val Amber = Color(0xFF8A5A00)

private val LightColors = lightColorScheme(
    primary = Navy,
    secondary = Teal,
    tertiary = Amber,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DB8DA),
    secondary = Color(0xFF7FCBB8),
    tertiary = Color(0xFFDBAE5C),
)

@Composable
fun SoporteMobileTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (useDarkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}

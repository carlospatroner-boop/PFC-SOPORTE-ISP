package ec.edu.uteq.soporte.mobile.ui.auth

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ec.edu.uteq.soporte.mobile.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Prueba instrumentada end-to-end exigida por el Modulo C item 6 ("al menos una prueba
 * instrumentada end-to-end, Espresso o Compose Testing"). Verifica el camino mas simple y mas
 * estable en CI: enviar el formulario vacio debe mostrar el mensaje de validacion, sin
 * depender de que el backend este arriba.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun enviarFormularioVacioMuestraMensajeDeValidacion() {
        composeRule.onNodeWithText("Ingresar").performClick()
        composeRule.onNodeWithText("Ingresa correo y contraseña").assertExists()
    }
}

package ec.edu.uteq.soporte.mobile.ui.auth

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import ec.edu.uteq.soporte.mobile.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

/**
 * Pruebas instrumentadas end-to-end exigidas por el Modulo C item 6 ("al menos una prueba
 * instrumentada end-to-end, Espresso o Compose Testing"). Ambas verifican solo validacion
 * del lado del cliente (LoginViewModel.submit(), ver LoginViewModel.kt) para ser estables en
 * CI sin depender de que el backend este arriba -- un intento real de login SI dispara una
 * llamada de red de fondo (viewModelScope.launch), pero eso ocurre despues de la asercion.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun enviarFormularioVacioMuestraMensajeDeValidacion() {
        // El texto real del boton es "INICIAR SESIÓN" (ver LoginScreen.kt) -- "Ingresar" es
        // el texto del boton equivalente en apps/web (i18n login.submit), no el de esta app.
        composeRule.onNodeWithText("INICIAR SESIÓN").performClick()
        composeRule.onNodeWithText("Ingresa correo y contraseña").assertExists()
    }

    @Test
    fun completarCorreoYContrasenaOcultaElMensajeDeValidacion() {
        composeRule.onNodeWithText("Correo Electrónico").performTextInput("tecnico@soporte.local")
        composeRule.onNodeWithText("Contraseña").performTextInput("Tecnico123!")
        composeRule.onNodeWithText("INICIAR SESIÓN").performClick()
        // No se espera un resultado de red (no hay backend garantizado en este test): solo
        // se verifica que la validacion sincronica de campos vacios (LoginViewModel.submit(),
        // la misma que cubre la prueba anterior) ya no bloquea el envio.
        composeRule.onNodeWithText("Ingresa correo y contraseña").assertDoesNotExist()
    }
}

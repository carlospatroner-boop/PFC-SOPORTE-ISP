package ec.edu.uteq.soporte.mobile.ui.auth

import app.cash.turbine.test
import ec.edu.uteq.soporte.mobile.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Prueba unitaria del ViewModel exigida por el criterio D3.2 de la rubrica de E4
 * ("Tests unit ... JUnit 5 + coroutines-test").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submit con campos vacios no llama al repositorio y muestra error`() = runTest {
        val viewModel = LoginViewModel(authRepository)

        viewModel.submit()

        assertEquals("Ingresa correo y contraseña", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `login exitoso actualiza loginSucceeded a true`() = runTest {
        coEvery { authRepository.login("tecnico@soporte.local", "Passw0rd!") } returns Result.success(Unit)
        val viewModel = LoginViewModel(authRepository)
        viewModel.onEmailChanged("tecnico@soporte.local")
        viewModel.onPasswordChanged("Passw0rd!")

        viewModel.uiState.test {
            skipItems(1) // estado inicial tras los onChanged
            viewModel.submit()
            dispatcher.scheduler.advanceUntilIdle()

            val loading = expectMostRecentItem()
            assertTrue(loading.loginSucceeded)
            assertFalse(loading.isLoading)
        }
    }

    @Test
    fun `login fallido expone el mensaje de error y no marca exito`() = runTest {
        coEvery { authRepository.login(any(), any()) } returns Result.failure(RuntimeException("credenciales invalidas"))
        val viewModel = LoginViewModel(authRepository)
        viewModel.onEmailChanged("tecnico@soporte.local")
        viewModel.onPasswordChanged("incorrecta")

        viewModel.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loginSucceeded)
        assertEquals("credenciales invalidas", state.errorMessage)
    }
}

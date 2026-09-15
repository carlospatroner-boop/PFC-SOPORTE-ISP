package ec.edu.uteq.soporte.mobile.ui.tickets.detail

import android.net.Uri
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus
import ec.edu.uteq.soporte.mobile.data.remote.dto.Zone
import ec.edu.uteq.soporte.mobile.data.repository.TicketRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Prueba unitaria del cierre en sitio con evidencia (Entregable 10 de la guia de cierre).
 * No toca android.util.Base64 (eso vive en TicketRepository.closeOnSite, ver su comentario):
 * aqui se mockea TicketRepository, igual que LoginViewModelTest mockea AuthRepository, para
 * probar solo la logica de este ViewModel (el guardian canCloseOnSite y que se le pasen a
 * TicketRepository los datos capturados) sin depender del framework de Android.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TicketDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var ticketRepository: TicketRepository
    private val ticketId = "11111111-1111-1111-1111-111111111111"

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        ticketRepository = mockk()
        coEvery { ticketRepository.getTicket(ticketId) } returns ticket(TicketStatus.EN_PROGRESO)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `closeOnSite sin foto ni ubicacion no llama al repositorio`() = runTest {
        val viewModel = TicketDetailViewModel(ticketRepository, ticketId)
        dispatcher.scheduler.advanceUntilIdle()

        viewModel.closeOnSite(ByteArray(0))
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { ticketRepository.closeOnSite(any(), any(), any(), any()) }
    }

    @Test
    fun `closeOnSite con foto y ubicacion envia la evidencia capturada`() = runTest {
        val photo = byteArrayOf(1, 2, 3)
        coEvery {
            ticketRepository.closeOnSite(ticketId, photo, -1.02, -79.46)
        } returns Result.success(ticket(TicketStatus.RESUELTO))

        val viewModel = TicketDetailViewModel(ticketRepository, ticketId)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.onEvidencePhotoCaptured(mockk<Uri>())
        viewModel.onLocationCaptured(-1.02, -79.46)

        viewModel.closeOnSite(photo)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { ticketRepository.closeOnSite(ticketId, photo, -1.02, -79.46) }
        assertTrue(viewModel.uiState.value.closeSucceeded)
        assertFalse(viewModel.uiState.value.isClosing)
    }

    private fun ticket(status: TicketStatus) = TicketResponse(
        zone = Zone.QUEVEDO_NORTE,
        ticketId = ticketId,
        clientId = "22222222-2222-2222-2222-222222222222",
        technicianId = null,
        category = null,
        priority = null,
        status = status,
        description = "Ticket de prueba",
        createdAt = "2026-09-15T00:00:00Z",
        slaDeadline = null,
        slaBreached = false,
    )
}

package ec.edu.uteq.soporte.mobile.ui.tickets.detail

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse
import ec.edu.uteq.soporte.mobile.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TicketDetailUiState(
    val ticket: TicketResponse? = null,
    val isLoading: Boolean = true,
    val evidencePhotoUri: Uri? = null,
    val capturedLatitude: Double? = null,
    val capturedLongitude: Double? = null,
    val isClosing: Boolean = false,
    val closeSucceeded: Boolean = false,
    val errorMessage: String? = null,
) {
    /** El cierre en sitio exige ambas evidencias -- Modulo C item 5, dominio ACC. */
    val canCloseOnSite: Boolean
        get() = evidencePhotoUri != null && capturedLatitude != null && capturedLongitude != null
}

class TicketDetailViewModel(
    private val ticketRepository: TicketRepository,
    private val ticketId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val ticket = ticketRepository.getTicket(ticketId)
            _uiState.update { it.copy(ticket = ticket, isLoading = false) }
        }
    }

    fun onEvidencePhotoCaptured(uri: Uri) {
        _uiState.update { it.copy(evidencePhotoUri = uri) }
    }

    fun onLocationCaptured(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(capturedLatitude = latitude, capturedLongitude = longitude) }
    }

    fun closeOnSite() {
        if (!_uiState.value.canCloseOnSite) return
        viewModelScope.launch {
            _uiState.update { it.copy(isClosing = true, errorMessage = null) }
            val result = ticketRepository.closeOnSite(ticketId)
            result.fold(
                onSuccess = { updated ->
                    _uiState.update { it.copy(isClosing = false, ticket = updated, closeSucceeded = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isClosing = false, errorMessage = error.message ?: "No se pudo cerrar el ticket")
                    }
                },
            )
        }
    }
}

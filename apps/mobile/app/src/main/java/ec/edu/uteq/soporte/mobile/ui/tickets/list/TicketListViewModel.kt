package ec.edu.uteq.soporte.mobile.ui.tickets.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketResponse
import ec.edu.uteq.soporte.mobile.data.remote.dto.TicketStatus
import ec.edu.uteq.soporte.mobile.data.repository.TicketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TicketListUiState(
    val tickets: List<TicketResponse> = emptyList(),
    val totalCount: Int = 0,
    val slaBreachedCount: Int = 0,
    val searchQuery: String = "",
    val statusFilter: TicketStatus? = null,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class TicketListViewModel(private val ticketRepository: TicketRepository) : ViewModel() {

    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<TicketStatus?>(null)

    val uiState: StateFlow<TicketListUiState> = combine(
        ticketRepository.observeTickets(),
        isRefreshing,
        errorMessage,
        searchQuery,
        statusFilter,
    ) { tickets, refreshing, error, query, status ->
        val filtered = tickets.filter { ticket ->
            val matchesQuery = query.isBlank() || ticket.description.contains(query, ignoreCase = true)
            val matchesStatus = status == null || ticket.status == status
            matchesQuery && matchesStatus
        }
        TicketListUiState(
            tickets = filtered,
            // Los conteos del resumen son sobre TODOS los tickets, no solo los filtrados --
            // el resumen responde "cómo estoy en general", no "cuántos veo ahora mismo".
            totalCount = tickets.size,
            slaBreachedCount = tickets.count { it.slaBreached },
            searchQuery = query,
            statusFilter = status,
            isRefreshing = refreshing,
            errorMessage = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TicketListUiState())

    init {
        refresh()
    }

    fun onSearchQueryChanged(value: String) {
        searchQuery.value = value
    }

    /** Tocar el mismo filtro otra vez lo quita (vuelve a "Todos"). */
    fun onStatusFilterSelected(status: TicketStatus?) {
        statusFilter.value = if (statusFilter.value == status) null else status
    }

    fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            val result = ticketRepository.refreshTickets()
            // Si falla por falta de red, no es un error bloqueante: el listado sigue
            // mostrando lo que ya esta en cache (modo sin conexion, Modulo C item 4).
            errorMessage.value = result.exceptionOrNull()?.let { "Sin conexión — mostrando datos guardados" }
            isRefreshing.value = false
        }
    }
}

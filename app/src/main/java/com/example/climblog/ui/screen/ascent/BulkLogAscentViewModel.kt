package com.example.climblog.ui.screen.ascent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.remote.BulkAscentParams
import com.example.climblog.data.remote.LezecCredentialsStore
import com.example.climblog.data.remote.LezecService
import com.example.climblog.data.remote.LezecSyncState
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BulkLogUiState(
    val routes: List<Route> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val style: AscentStyle = AscentStyle.REDPOINT,
    val attempts: Int = 1,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val syncToLezec: Boolean = false,
    val lezecHasCredentials: Boolean = false,
    val lezecSyncState: LezecSyncState = LezecSyncState.IDLE,
)

@HiltViewModel
class BulkLogAscentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository,
    private val lezecService: LezecService,
    private val lezecCredentialsStore: LezecCredentialsStore,
) : ViewModel() {

    private val routeIds: List<Long> = savedStateHandle.get<String>("routeIds")
        ?.split(",")
        ?.mapNotNull { it.toLongOrNull() }
        ?: emptyList()

    private val _uiState = MutableStateFlow(BulkLogUiState())
    val uiState: StateFlow<BulkLogUiState> = _uiState.asStateFlow()

    val navigateToSettings = Channel<Unit>(Channel.BUFFERED)

    init {
        viewModelScope.launch {
            lezecCredentialsStore.hasCredentialsFlow.collect { has ->
                _uiState.update { it.copy(lezecHasCredentials = has) }
            }
        }
        viewModelScope.launch {
            val routes = routeIds.mapNotNull { routeRepository.getRouteById(it) }
            _uiState.update { it.copy(routes = routes, isLoading = false) }
        }
    }

    fun onDateChange(millis: Long) = _uiState.update { it.copy(date = millis) }
    fun onStyleChange(style: AscentStyle) = _uiState.update { it.copy(style = style) }
    fun onAttemptsChange(attempts: Int) = _uiState.update { it.copy(attempts = attempts.coerceAtLeast(1)) }

    fun onSyncToLezecToggle(enabled: Boolean) {
        if (enabled && !lezecCredentialsStore.hasCredentials()) {
            viewModelScope.launch { navigateToSettings.send(Unit) }
        } else {
            _uiState.update { it.copy(syncToLezec = enabled) }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true, lezecSyncState = LezecSyncState.IDLE) }
        viewModelScope.launch {
            state.routes.forEach { route ->
                ascentRepository.saveAscent(
                    Ascent(
                        routeId = route.id,
                        date = state.date,
                        style = state.style,
                        attempts = state.attempts
                    )
                )
            }

            if (state.syncToLezec) {
                val creds = lezecCredentialsStore.get()
                if (creds != null) {
                    _uiState.update { it.copy(lezecSyncState = LezecSyncState.SYNCING) }
                    val ascentsToSync = state.routes.mapNotNull { route ->
                        route.lezecId?.let { lezecId ->
                            BulkAscentParams(
                                lezecId = lezecId,
                                dateMillis = state.date,
                                style = state.style,
                                grade = route.grade,
                                attempts = state.attempts,
                                note = "",
                                routeType = route.type
                            )
                        }
                    }
                    if (ascentsToSync.isEmpty()) {
                        _uiState.update { it.copy(lezecSyncState = LezecSyncState.NO_ID) }
                        kotlinx.coroutines.delay(1500)
                    } else {
                        val results = runCatching {
                            lezecService.loginAndLogAscents(creds.first, creds.second, ascentsToSync)
                        }.getOrDefault(emptyList())
                        val allOk = results.isNotEmpty() && results.all { it }
                        _uiState.update { it.copy(lezecSyncState = if (allOk) LezecSyncState.SUCCESS else LezecSyncState.FAILED) }
                        if (!allOk) kotlinx.coroutines.delay(1500)
                    }
                }
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

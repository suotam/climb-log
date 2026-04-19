package com.example.climblog.ui.screen.ascent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val isSaved: Boolean = false
)

@HiltViewModel
class BulkLogAscentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository
) : ViewModel() {

    private val routeIds: List<Long> = savedStateHandle.get<String>("routeIds")
        ?.split(",")
        ?.mapNotNull { it.toLongOrNull() }
        ?: emptyList()

    private val _uiState = MutableStateFlow(BulkLogUiState())
    val uiState: StateFlow<BulkLogUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val routes = routeIds.mapNotNull { routeRepository.getRouteById(it) }
            _uiState.update { it.copy(routes = routes, isLoading = false) }
        }
    }

    fun onDateChange(millis: Long) = _uiState.update { it.copy(date = millis) }
    fun onStyleChange(style: AscentStyle) = _uiState.update { it.copy(style = style) }
    fun onAttemptsChange(attempts: Int) = _uiState.update { it.copy(attempts = attempts.coerceAtLeast(1)) }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
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
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

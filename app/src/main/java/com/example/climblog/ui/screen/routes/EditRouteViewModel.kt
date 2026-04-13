package com.example.climblog.ui.screen.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.GradeSystem
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.RouteType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditRouteUiState(
    val name: String = "",
    val grade: String = "",
    val gradeSystem: GradeSystem = GradeSystem.FRENCH,
    val type: RouteType = RouteType.SPORT,
    val lengthText: String = "",
    val boltsText: String = "",
    val description: String = "",
    val firstAscent: String = "",
    val firstAscentYearText: String = "",
    val isEditMode: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
) {
    val canSave: Boolean get() = name.isNotBlank() && grade.isNotBlank()
}

@HiltViewModel
class EditRouteViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository
) : ViewModel() {

    private val sectorId: Long = checkNotNull(savedStateHandle["sectorId"])
    private val routeId: Long = checkNotNull(savedStateHandle["routeId"])

    private val _state = MutableStateFlow(EditRouteUiState())
    val uiState: StateFlow<EditRouteUiState> = _state.asStateFlow()

    private var existingRoute: Route? = null

    init {
        if (routeId != -1L) {
            viewModelScope.launch {
                val route = routeRepository.getRouteById(routeId)
                if (route != null) {
                    existingRoute = route
                    _state.update {
                        it.copy(
                            name = route.name,
                            grade = route.grade,
                            gradeSystem = route.gradeSystem,
                            type = route.type,
                            lengthText = route.length?.toString() ?: "",
                            boltsText = route.bolts?.toString() ?: "",
                            description = route.description,
                            firstAscent = route.firstAscent ?: "",
                            firstAscentYearText = route.firstAscentYear?.toString() ?: "",
                            isEditMode = true,
                            isLoading = false
                        )
                    }
                }
            }
        } else {
            _state.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChange(v: String) = _state.update { it.copy(name = v) }
    fun onGradeChange(v: String) = _state.update { it.copy(grade = v) }
    fun onGradeSystemChange(v: GradeSystem) = _state.update { it.copy(gradeSystem = v) }
    fun onTypeChange(v: RouteType) = _state.update { it.copy(type = v) }
    fun onLengthChange(v: String) = _state.update { it.copy(lengthText = v.filter { c -> c.isDigit() }) }
    fun onBoltsChange(v: String) = _state.update { it.copy(boltsText = v.filter { c -> c.isDigit() }) }
    fun onDescriptionChange(v: String) = _state.update { it.copy(description = v) }
    fun onFirstAscentChange(v: String) = _state.update { it.copy(firstAscent = v) }
    fun onFirstAscentYearChange(v: String) = _state.update { it.copy(firstAscentYearText = v.filter { c -> c.isDigit() }.take(4)) }

    fun save() {
        val s = _state.value
        if (!s.canSave || s.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val route = Route(
                id = existingRoute?.id ?: 0L,
                sectorId = existingRoute?.sectorId ?: sectorId,
                name = s.name.trim(),
                grade = s.grade.trim(),
                gradeSystem = s.gradeSystem,
                type = s.type,
                length = s.lengthText.toIntOrNull(),
                bolts = s.boltsText.toIntOrNull(),
                description = s.description.trim(),
                firstAscent = s.firstAscent.trim().takeIf { it.isNotBlank() },
                firstAscentYear = s.firstAscentYearText.toIntOrNull()
            )
            if (s.isEditMode) routeRepository.updateRoute(route)
            else routeRepository.saveRoute(route)
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

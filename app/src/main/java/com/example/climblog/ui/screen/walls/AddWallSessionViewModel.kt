package com.example.climblog.ui.screen.walls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.WallRepository
import com.example.climblog.domain.model.BoulderGradeSystem
import com.example.climblog.domain.model.RopeGradeSystem
import com.example.climblog.domain.model.SessionType
import com.example.climblog.domain.model.Wall
import com.example.climblog.domain.model.gradesForType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddWallSessionUiState(
    val walls: List<Wall> = emptyList(),
    val selectedWall: Wall? = null,
    val newWallName: String = "",
    val showNewWallField: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val type: SessionType = SessionType.BOULDER,
    val ropeGradeSystem: RopeGradeSystem = RopeGradeSystem.FRENCH,
    val boulderGradeSystem: BoulderGradeSystem = BoulderGradeSystem.FRENCH,
    val grades: Map<String, Int> = emptyMap(), // grade → count
    val notes: String = "",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    val canSave: Boolean get() = selectedWall != null && grades.values.any { it > 0 }
    val totalRoutes: Int get() = grades.values.sum()
    val currentGrades: List<String> get() = gradesForType(type, ropeGradeSystem, boulderGradeSystem)
}

@HiltViewModel
class AddWallSessionViewModel @Inject constructor(
    private val wallRepository: WallRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddWallSessionUiState())
    val uiState: StateFlow<AddWallSessionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            wallRepository.getAllWalls().collect { walls ->
                _state.update { s ->
                    s.copy(
                        walls = walls,
                        selectedWall = s.selectedWall ?: walls.firstOrNull()
                    )
                }
            }
        }
    }

    fun selectWall(wall: Wall) = _state.update { it.copy(selectedWall = wall, showNewWallField = false) }

    fun onNewWallNameChange(name: String) = _state.update { it.copy(newWallName = name) }

    fun toggleNewWallField() = _state.update { it.copy(showNewWallField = !it.showNewWallField, newWallName = "") }

    fun saveNewWall() {
        val name = _state.value.newWallName.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = wallRepository.saveWall(Wall(name = name))
            val newWall = Wall(id = id, name = name)
            _state.update { it.copy(newWallName = "", showNewWallField = false, selectedWall = newWall) }
        }
    }

    fun deleteWall(wall: Wall) {
        viewModelScope.launch {
            wallRepository.deleteWall(wall)
            _state.update { s ->
                s.copy(selectedWall = if (s.selectedWall?.id == wall.id) null else s.selectedWall)
            }
        }
    }

    fun onDateChange(millis: Long) = _state.update { it.copy(date = millis) }

    fun onTypeChange(type: SessionType) {
        _state.update { it.copy(type = type, grades = emptyMap()) }
    }

    fun onRopeGradeSystemChange(system: RopeGradeSystem) {
        _state.update { it.copy(ropeGradeSystem = system, grades = emptyMap()) }
    }

    fun onBoulderGradeSystemChange(system: BoulderGradeSystem) {
        _state.update { it.copy(boulderGradeSystem = system, grades = emptyMap()) }
    }

    fun incrementGrade(grade: String) {
        _state.update { s ->
            val current = s.grades[grade] ?: 0
            s.copy(grades = s.grades + (grade to current + 1))
        }
    }

    fun decrementGrade(grade: String) {
        _state.update { s ->
            val current = s.grades[grade] ?: 0
            if (current <= 1) s.copy(grades = s.grades - grade)
            else s.copy(grades = s.grades + (grade to current - 1))
        }
    }

    fun onNotesChange(notes: String) = _state.update { it.copy(notes = notes) }

    fun save() {
        val s = _state.value
        if (!s.canSave || s.isSaving) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            wallRepository.saveSession(
                wallId = s.selectedWall!!.id,
                date = s.date,
                type = s.type,
                notes = s.notes,
                grades = s.grades
            )
            _state.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

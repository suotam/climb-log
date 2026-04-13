package com.example.climblog.ui.screen.ascent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Photo
import com.example.climblog.domain.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LogAscentUiState(
    val route: Route? = null,
    val date: Long = System.currentTimeMillis(),
    val style: AscentStyle = AscentStyle.REDPOINT,
    val attempts: Int = 1,
    val personalNote: String = "",
    val publicNote: String = "",
    val personalGrade: String? = null,
    val rating: Int? = null,
    // Photos already in DB (edit mode)
    val existingPhotos: List<Photo> = emptyList(),
    // URIs staged to be saved when ascent is saved (both new and edit mode)
    val pendingPhotoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class LogAscentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val routeId: Long = checkNotNull(savedStateHandle["routeId"])
    private val editAscentId: Long? = savedStateHandle.get<Long>("ascentId")?.takeIf { it != -1L }

    private val _uiState = MutableStateFlow(LogAscentUiState())
    val uiState: StateFlow<LogAscentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val route = routeRepository.getRouteById(routeId)
            _uiState.update { it.copy(route = route) }
        }
        if (editAscentId != null) {
            // Load existing photos for this ascent in edit mode
            viewModelScope.launch {
                photoRepository.getPhotosByAscent(editAscentId).collect { photos ->
                    _uiState.update { it.copy(existingPhotos = photos) }
                }
            }
        }
    }

    fun onDateChange(millis: Long) = _uiState.update { it.copy(date = millis) }
    fun onStyleChange(style: AscentStyle) = _uiState.update { it.copy(style = style) }
    fun onAttemptsChange(attempts: Int) = _uiState.update { it.copy(attempts = attempts.coerceAtLeast(1)) }
    fun onPersonalNoteChange(note: String) = _uiState.update { it.copy(personalNote = note) }
    fun onPublicNoteChange(note: String) = _uiState.update { it.copy(publicNote = note) }
    fun onPersonalGradeChange(grade: String) = _uiState.update { it.copy(personalGrade = grade.ifBlank { null }) }
    fun onRatingChange(rating: Int?) = _uiState.update { it.copy(rating = rating) }

    fun addPhotos(uris: List<String>) {
        _uiState.update { it.copy(pendingPhotoUris = it.pendingPhotoUris + uris) }
    }

    fun deletePendingPhoto(uri: String) {
        _uiState.update { it.copy(pendingPhotoUris = it.pendingPhotoUris - uri) }
    }

    fun deleteExistingPhoto(photo: Photo) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val ascent = Ascent(
                id = editAscentId ?: 0L,
                routeId = routeId,
                date = state.date,
                style = state.style,
                attempts = state.attempts,
                personalNote = state.personalNote,
                publicNote = state.publicNote,
                personalGrade = state.personalGrade,
                rating = state.rating
            )
            val savedId = if (editAscentId != null) {
                ascentRepository.updateAscent(ascent)
                editAscentId
            } else {
                ascentRepository.saveAscent(ascent)
            }
            // Persist any staged photos now that we have the ascentId
            state.pendingPhotoUris.forEach { uri ->
                photoRepository.savePhoto(Photo(ascentId = savedId, uri = uri))
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

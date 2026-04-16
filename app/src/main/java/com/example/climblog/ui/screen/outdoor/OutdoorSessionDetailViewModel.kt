package com.example.climblog.ui.screen.outdoor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.OutdoorSessionRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.domain.model.OutdoorSession
import com.example.climblog.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OutdoorSessionDetailUiState(
    val session: OutdoorSession? = null,
    val photos: List<Photo> = emptyList(),
    val notes: String = "",
    val pendingPhotoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class OutdoorSessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepository: OutdoorSessionRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val sessionId: Long = checkNotNull(savedStateHandle["sessionId"])

    private val _notes = MutableStateFlow("")
    private val _pendingPhotoUris = MutableStateFlow<List<String>>(emptyList())
    private val _isSaving = MutableStateFlow(false)

    private val sessionFlow = sessionRepository.getSessionById(sessionId)
    private val photosFlow = photoRepository.getPhotosByOutdoorSession(sessionId)

    val uiState: StateFlow<OutdoorSessionDetailUiState> = combine(
        sessionFlow,
        photosFlow,
        _notes,
        _pendingPhotoUris,
        _isSaving
    ) { session, photos, notes, pending, saving ->
        OutdoorSessionDetailUiState(
            session = session,
            photos = photos,
            notes = notes,
            pendingPhotoUris = pending,
            isSaving = saving,
            isLoading = session == null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = OutdoorSessionDetailUiState()
    )

    init {
        // Pre-fill notes from session when first loaded
        viewModelScope.launch {
            sessionFlow.first { it != null }?.let { session ->
                _notes.value = session.notes
            }
        }
    }

    fun onNotesChange(notes: String) { _notes.value = notes }

    fun addPhotos(uris: List<String>) {
        _pendingPhotoUris.update { it + uris }
    }

    fun removePendingPhoto(uri: String) {
        _pendingPhotoUris.update { it - uri }
    }

    fun deleteExistingPhoto(photo: Photo) {
        viewModelScope.launch { photoRepository.deletePhoto(photo) }
    }

    fun save() {
        val state = uiState.value
        if (state.isSaving) return
        _isSaving.value = true
        viewModelScope.launch {
            sessionRepository.updateNotes(sessionId, _notes.value)
            _pendingPhotoUris.value.forEach { uri ->
                photoRepository.savePhoto(Photo(outdoorSessionId = sessionId, uri = uri))
            }
            _pendingPhotoUris.value = emptyList()
            _isSaving.value = false
        }
    }
}

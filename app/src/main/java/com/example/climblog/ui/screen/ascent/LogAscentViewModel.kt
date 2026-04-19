package com.example.climblog.ui.screen.ascent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.remote.LezecCredentialsStore
import com.example.climblog.data.remote.LezecService
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Photo
import com.example.climblog.domain.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LezecSyncState { IDLE, SYNCING, SUCCESS, FAILED, NO_ID }

data class LogAscentUiState(
    val route: Route? = null,
    val date: Long = System.currentTimeMillis(),
    val style: AscentStyle = AscentStyle.REDPOINT,
    val attempts: Int = 1,
    val personalNote: String = "",
    val publicNote: String = "",
    val personalGrade: String? = null,
    val rating: Int? = null,
    val existingPhotos: List<Photo> = emptyList(),
    val pendingPhotoUris: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    // Lezec sync
    val syncToLezec: Boolean = false,
    val lezecHasCredentials: Boolean = false,
    val showCredentialsDialog: Boolean = false,
    val lezecSyncState: LezecSyncState = LezecSyncState.IDLE,
)

@HiltViewModel
class LogAscentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository,
    private val photoRepository: PhotoRepository,
    private val lezecService: LezecService,
    private val lezecCredentialsStore: LezecCredentialsStore,
) : ViewModel() {

    private val routeId: Long = checkNotNull(savedStateHandle["routeId"])
    private val editAscentId: Long? = savedStateHandle.get<Long>("ascentId")?.takeIf { it != -1L }

    private var existingOutdoorSessionId: Long? = null

    private val _uiState = MutableStateFlow(LogAscentUiState())
    val uiState: StateFlow<LogAscentUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(lezecHasCredentials = lezecCredentialsStore.hasCredentials()) }
        viewModelScope.launch {
            val route = routeRepository.getRouteById(routeId)
            _uiState.update { it.copy(route = route) }
        }
        if (editAscentId != null) {
            viewModelScope.launch {
                val existing = ascentRepository.getAscentById(editAscentId)
                if (existing != null) {
                    existingOutdoorSessionId = existing.outdoorSessionId
                    _uiState.update {
                        it.copy(
                            date = existing.date,
                            style = existing.style,
                            attempts = existing.attempts,
                            personalNote = existing.personalNote,
                            publicNote = existing.publicNote,
                            personalGrade = existing.personalGrade,
                            rating = existing.rating
                        )
                    }
                }
            }
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
        viewModelScope.launch { photoRepository.deletePhoto(photo) }
    }

    fun onSyncToLezecToggle(enabled: Boolean) {
        if (enabled && !lezecCredentialsStore.hasCredentials()) {
            _uiState.update { it.copy(showCredentialsDialog = true) }
        } else {
            _uiState.update { it.copy(syncToLezec = enabled) }
        }
    }

    fun onCredentialsSaved(uid: String, password: String) {
        lezecCredentialsStore.save(uid, password)
        _uiState.update { it.copy(lezecHasCredentials = true, syncToLezec = true, showCredentialsDialog = false) }
    }

    fun onCredentialsDismissed() {
        _uiState.update { it.copy(showCredentialsDialog = false) }
    }

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        _uiState.update { it.copy(isSaving = true, lezecSyncState = LezecSyncState.IDLE) }
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
                rating = state.rating,
                outdoorSessionId = existingOutdoorSessionId
            )
            val savedId = if (editAscentId != null) {
                ascentRepository.updateAscent(ascent)
                editAscentId
            } else {
                ascentRepository.saveAscent(ascent)
            }
            state.pendingPhotoUris.forEach { uri ->
                photoRepository.savePhoto(Photo(ascentId = savedId, uri = uri))
            }

            if (state.syncToLezec) {
                val lezecId = state.route?.lezecId
                if (lezecId == null) {
                    _uiState.update { it.copy(lezecSyncState = LezecSyncState.NO_ID) }
                    delay(1500)
                } else {
                    _uiState.update { it.copy(lezecSyncState = LezecSyncState.SYNCING) }
                    val creds = lezecCredentialsStore.get()
                    val success = if (creds != null) {
                        runCatching {
                            lezecService.loginAndLogAscent(
                                uid = creds.first,
                                password = creds.second,
                                lezecId = lezecId,
                                dateMillis = state.date,
                                style = state.style,
                                grade = state.personalGrade ?: state.route?.grade ?: "",
                                attempts = state.attempts,
                                note = state.publicNote,
                                routeType = state.route?.type ?: com.example.climblog.domain.model.RouteType.SPORT
                            )
                        }.getOrDefault(false)
                    } else false
                    _uiState.update { it.copy(lezecSyncState = if (success) LezecSyncState.SUCCESS else LezecSyncState.FAILED) }
                    if (!success) delay(1500)
                }
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

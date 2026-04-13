package com.example.climblog.ui.screen.areas

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.domain.model.Area
import com.example.climblog.domain.model.Photo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AreaDetailUiState(
    val area: Area? = null,
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AreaDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val areaRepository: AreaRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val areaId: Long = checkNotNull(savedStateHandle["areaId"])

    private val _area = MutableStateFlow<Area?>(null)

    val uiState: StateFlow<AreaDetailUiState> = combine(
        _area,
        photoRepository.getPhotosByArea(areaId)
    ) { area, photos ->
        AreaDetailUiState(area = area, photos = photos, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AreaDetailUiState()
    )

    init {
        viewModelScope.launch {
            _area.value = areaRepository.getAreaById(areaId)
        }
    }

    fun addPhotos(uris: List<String>) {
        viewModelScope.launch {
            uris.forEach { uri ->
                photoRepository.savePhoto(Photo(areaId = areaId, uri = uri))
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
        }
    }
}

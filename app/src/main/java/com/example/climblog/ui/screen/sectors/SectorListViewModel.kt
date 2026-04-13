package com.example.climblog.ui.screen.sectors

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.domain.model.Area
import com.example.climblog.domain.model.Sector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SectorListUiState(
    val area: Area? = null,
    val sectors: List<Sector> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SectorListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val areaRepository: AreaRepository
) : ViewModel() {

    private val areaId: Long = checkNotNull(savedStateHandle["areaId"])

    private val _area = MutableStateFlow<Area?>(null)

    val uiState: StateFlow<SectorListUiState> = combine(
        _area,
        areaRepository.getSectorsByArea(areaId)
    ) { area, sectors ->
        SectorListUiState(area = area, sectors = sectors, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SectorListUiState()
    )

    init {
        viewModelScope.launch {
            _area.value = areaRepository.getAreaById(areaId)
        }
    }
}

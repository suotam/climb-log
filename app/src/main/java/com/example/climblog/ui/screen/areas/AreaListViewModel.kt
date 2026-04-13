package com.example.climblog.ui.screen.areas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.domain.model.Area
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AreaListUiState(
    val areas: List<Area> = emptyList(),          // filtered (list mode)
    val allAreas: List<Area> = emptyList(),        // unfiltered (map mode)
    val searchQuery: String = "",
    val showMap: Boolean = false,
    val selectedAreaId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class AreaListViewModel @Inject constructor(
    private val areaRepository: AreaRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _showMap = MutableStateFlow(false)
    private val _selectedAreaId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AreaListUiState> = combine(
        areaRepository.getAllAreas(),
        _searchQuery,
        _showMap,
        _selectedAreaId
    ) { areas, query, showMap, selectedId ->
        val filtered = if (query.isBlank()) areas
        else areas.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.region.contains(query, ignoreCase = true)
        }
        AreaListUiState(
            areas = filtered,
            allAreas = areas,
            searchQuery = query,
            showMap = showMap,
            selectedAreaId = selectedId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AreaListUiState()
    )

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }

    fun toggleMapView() {
        _showMap.update { !it }
        _selectedAreaId.value = null
    }

    fun selectAreaOnMap(areaId: Long) {
        _selectedAreaId.value = if (_selectedAreaId.value == areaId) null else areaId
    }

    fun clearMapSelection() { _selectedAreaId.value = null }
}

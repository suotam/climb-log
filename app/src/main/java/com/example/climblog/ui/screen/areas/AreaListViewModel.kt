package com.example.climblog.ui.screen.areas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.dao.RouteWithContext
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.dao.SectorWithArea
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.domain.model.Area
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class SearchMode { AREAS, SECTORS, ROUTES }

data class AreaListUiState(
    val areas: List<Area> = emptyList(),
    val allAreas: List<Area> = emptyList(),
    val searchQuery: String = "",
    val searchMode: SearchMode = SearchMode.AREAS,
    val sectorResults: List<SectorWithArea> = emptyList(),
    val routeResults: List<RouteWithContext> = emptyList(),
    val showMap: Boolean = false,
    val selectedAreaId: Long? = null,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AreaListViewModel @Inject constructor(
    private val areaRepository: AreaRepository,
    private val sectorDao: SectorDao,
    private val routeDao: RouteDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _searchMode = MutableStateFlow(SearchMode.AREAS)
    private val _showMap = MutableStateFlow(false)
    private val _selectedAreaId = MutableStateFlow<Long?>(null)

    private val sectorResultsFlow: Flow<List<SectorWithArea>> =
        combine(_searchQuery, _searchMode) { q, mode -> q to mode }
            .flatMapLatest { (q, mode) ->
                if (mode == SearchMode.SECTORS && q.length >= 2)
                    sectorDao.searchSectors("%$q%")
                else
                    flowOf(emptyList())
            }

    private val routeResultsFlow: Flow<List<RouteWithContext>> =
        combine(_searchQuery, _searchMode) { q, mode -> q to mode }
            .flatMapLatest { (q, mode) ->
                if (mode == SearchMode.ROUTES && q.length >= 2)
                    routeDao.searchRoutes("%$q%")
                else
                    flowOf(emptyList())
            }

    val uiState: StateFlow<AreaListUiState> = combine(
        combine(areaRepository.getAllAreas(), _searchQuery) { areas, q -> areas to q },
        combine(_showMap, _selectedAreaId) { m, s -> m to s },
        combine(_searchMode, sectorResultsFlow, routeResultsFlow) { mode, sec, rts -> Triple(mode, sec, rts) }
    ) { (areas, q), (showMap, selId), (mode, sectors, routes) ->
        val filtered = if (q.isBlank()) areas
        else areas.filter {
            it.name.contains(q, ignoreCase = true) || it.region.contains(q, ignoreCase = true)
        }
        AreaListUiState(
            areas = filtered,
            allAreas = areas,
            searchQuery = q,
            searchMode = mode,
            sectorResults = sectors,
            routeResults = routes,
            showMap = showMap,
            selectedAreaId = selId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AreaListUiState()
    )

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onSearchModeChange(mode: SearchMode) {
        _searchMode.value = mode
        _searchQuery.value = ""
    }

    fun toggleMapView() {
        _showMap.update { !it }
        _selectedAreaId.value = null
    }

    fun selectAreaOnMap(areaId: Long) {
        _selectedAreaId.value = if (_selectedAreaId.value == areaId) null else areaId
    }

    fun clearMapSelection() { _selectedAreaId.value = null }
}

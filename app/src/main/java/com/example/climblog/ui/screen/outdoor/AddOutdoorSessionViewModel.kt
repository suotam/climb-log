package com.example.climblog.ui.screen.outdoor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.remote.BulkAscentParams
import com.example.climblog.data.remote.LezecCredentialsStore
import com.example.climblog.data.remote.LezecService
import com.example.climblog.data.remote.LezecSyncState
import com.example.climblog.data.repository.OutdoorSessionRepository
import com.example.climblog.domain.model.Area
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.RouteType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CragMode { KNOWN_AREA, CUSTOM_CRAG }

data class SelectableRoute(
    val routeId: Long,
    val routeName: String,
    val grade: String,
    val lezecId: Int? = null,
    val routeType: RouteType = RouteType.SPORT,
    val isSelected: Boolean = false,
    val style: AscentStyle = AscentStyle.REDPOINT
)

data class SectorWithRoutes(
    val sectorId: Long,
    val sectorName: String,
    val routes: List<SelectableRoute>
)

data class AddOutdoorSessionUiState(
    val mode: CragMode = CragMode.KNOWN_AREA,
    val areas: List<Area> = emptyList(),
    val areaSearch: String = "",
    val selectedArea: Area? = null,
    val sectorsWithRoutes: List<SectorWithRoutes> = emptyList(),
    val isLoadingRoutes: Boolean = false,
    val customCragName: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val pendingPhotoUris: List<String> = emptyList(),
    val syncToLezec: Boolean = false,
    val lezecHasCredentials: Boolean = false,
    val lezecSyncState: LezecSyncState = LezecSyncState.IDLE,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
) {
    val canSave: Boolean get() = when (mode) {
        CragMode.KNOWN_AREA -> selectedArea != null
        CragMode.CUSTOM_CRAG -> customCragName.isNotBlank()
    }

    val filteredAreas: List<Area> get() = if (areaSearch.isBlank()) areas
    else areas.filter { it.name.contains(areaSearch, ignoreCase = true) }

    val selectedRoutes: List<Pair<Long, AscentStyle>> get() =
        sectorsWithRoutes.flatMap { s -> s.routes.filter { it.isSelected }.map { it.routeId to it.style } }
}

@HiltViewModel
class AddOutdoorSessionViewModel @Inject constructor(
    private val repository: OutdoorSessionRepository,
    private val areaDao: AreaDao,
    private val sectorDao: SectorDao,
    private val routeDao: RouteDao,
    private val lezecService: LezecService,
    private val lezecCredentialsStore: LezecCredentialsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddOutdoorSessionUiState())
    val uiState: StateFlow<AddOutdoorSessionUiState> = _uiState

    val navigateToSettings = Channel<Unit>(Channel.BUFFERED)

    private var routeLoadingJob: Job? = null

    init {
        viewModelScope.launch {
            lezecCredentialsStore.hasCredentialsFlow.collect { has ->
                _uiState.update { it.copy(lezecHasCredentials = has) }
            }
        }
        viewModelScope.launch {
            val areas = areaDao.getAllAreas().first().map { it.toDomain() }
            _uiState.update { it.copy(areas = areas) }
        }
    }

    fun onModeChange(mode: CragMode) {
        _uiState.update { it.copy(mode = mode, selectedArea = null, sectorsWithRoutes = emptyList(), areaSearch = "") }
    }

    fun onAreaSearchChange(query: String) {
        _uiState.update { it.copy(areaSearch = query) }
    }

    fun selectArea(area: Area) {
        _uiState.update { it.copy(selectedArea = area, areaSearch = area.name, sectorsWithRoutes = emptyList(), isLoadingRoutes = true) }
        routeLoadingJob?.cancel()
        routeLoadingJob = viewModelScope.launch {
            val sectors = sectorDao.getSectorsByArea(area.id).first()
            val sectorsWithRoutes = sectors.map { sector ->
                val routes = routeDao.getRoutesBySector(sector.id).first()
                SectorWithRoutes(
                    sectorId = sector.id,
                    sectorName = sector.name,
                    routes = routes.map { route ->
                        SelectableRoute(
                            routeId = route.id,
                            routeName = route.name,
                            grade = route.grade,
                            lezecId = route.lezecId,
                            routeType = RouteType.valueOf(route.type)
                        )
                    }
                )
            }
            _uiState.update { it.copy(sectorsWithRoutes = sectorsWithRoutes, isLoadingRoutes = false) }
        }
    }

    fun toggleRoute(sectorId: Long, routeId: Long) {
        _uiState.update { state ->
            state.copy(sectorsWithRoutes = state.sectorsWithRoutes.map { sector ->
                if (sector.sectorId != sectorId) sector
                else sector.copy(routes = sector.routes.map { route ->
                    if (route.routeId != routeId) route
                    else route.copy(isSelected = !route.isSelected)
                })
            })
        }
    }

    fun setRouteStyle(sectorId: Long, routeId: Long, style: AscentStyle) {
        _uiState.update { state ->
            state.copy(sectorsWithRoutes = state.sectorsWithRoutes.map { sector ->
                if (sector.sectorId != sectorId) sector
                else sector.copy(routes = sector.routes.map { route ->
                    if (route.routeId != routeId) route else route.copy(style = style)
                })
            })
        }
    }

    fun onCustomCragNameChange(name: String) { _uiState.update { it.copy(customCragName = name) } }
    fun onDateChange(date: Long) { _uiState.update { it.copy(date = date) } }
    fun onNotesChange(notes: String) { _uiState.update { it.copy(notes = notes) } }

    fun onSyncToLezecToggle(enabled: Boolean) {
        if (enabled && !lezecCredentialsStore.hasCredentials()) {
            viewModelScope.launch { navigateToSettings.send(Unit) }
        } else {
            _uiState.update { it.copy(syncToLezec = enabled) }
        }
    }

    fun addPhotos(uris: List<String>) {
        _uiState.update { it.copy(pendingPhotoUris = it.pendingPhotoUris + uris) }
    }

    fun removePhoto(uri: String) {
        _uiState.update { it.copy(pendingPhotoUris = it.pendingPhotoUris - uri) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave || state.isSaving) return
        _uiState.update { it.copy(isSaving = true, lezecSyncState = LezecSyncState.IDLE) }
        viewModelScope.launch {
            repository.saveSession(
                date = state.date,
                areaId = if (state.mode == CragMode.KNOWN_AREA) state.selectedArea?.id else null,
                customCragName = if (state.mode == CragMode.CUSTOM_CRAG) state.customCragName.trim() else null,
                notes = state.notes.trim(),
                routes = state.selectedRoutes,
                photoUris = state.pendingPhotoUris
            )

            if (state.syncToLezec) {
                val creds = lezecCredentialsStore.get()
                if (creds != null) {
                    _uiState.update { it.copy(lezecSyncState = LezecSyncState.SYNCING) }
                    val selectedRouteMap = state.sectorsWithRoutes
                        .flatMap { it.routes }
                        .filter { it.isSelected && it.lezecId != null }
                    val ascentsToSync = selectedRouteMap.map { route ->
                        BulkAscentParams(
                            lezecId = route.lezecId!!,
                            dateMillis = state.date,
                            style = route.style,
                            grade = route.grade,
                            attempts = 1,
                            note = "",
                            routeType = route.routeType
                        )
                    }
                    if (ascentsToSync.isEmpty()) {
                        _uiState.update { it.copy(lezecSyncState = LezecSyncState.NO_ID) }
                        delay(1500)
                    } else {
                        val results = runCatching {
                            lezecService.loginAndLogAscents(creds.first, creds.second, ascentsToSync)
                        }.getOrDefault(emptyList())
                        val allOk = results.isNotEmpty() && results.all { it }
                        _uiState.update { it.copy(lezecSyncState = if (allOk) LezecSyncState.SUCCESS else LezecSyncState.FAILED) }
                        if (!allOk) delay(1500)
                    }
                }
            }

            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }
}

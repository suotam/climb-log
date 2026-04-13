package com.example.climblog.ui.screen.routes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.Sector
import com.example.climblog.domain.model.priority
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RouteListUiState(
    val sector: Sector? = null,
    val routes: List<RouteWithStatus> = emptyList(),
    val isLoading: Boolean = true
)

data class RouteWithStatus(
    val route: Route,
    val isSent: Boolean,
    val bestStyle: AscentStyle?
)

@HiltViewModel
class RouteListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val areaRepository: AreaRepository,
    private val routeRepository: RouteRepository,
    private val ascentRepository: AscentRepository
) : ViewModel() {

    private val sectorId: Long = checkNotNull(savedStateHandle["sectorId"])

    private val _sector = MutableStateFlow<Sector?>(null)

    // Načteme všechny přelezy najednou a pak matchujeme s cestami
    private val allAscents = ascentRepository.getAllAscents()

    val uiState: StateFlow<RouteListUiState> = combine(
        _sector,
        routeRepository.getRoutesBySector(sectorId),
        allAscents
    ) { sector, routes, ascents ->
        val ascentsByRoute = ascents.groupBy { it.routeId }
        val routesWithStatus = routes.map { route ->
            val routeAscents = ascentsByRoute[route.id] ?: emptyList()
            val bestAscent = routeAscents
                .filter { it.style != AscentStyle.ATTEMPT && it.style != AscentStyle.PROJECT }
                .minByOrNull { it.style.priority }
            RouteWithStatus(
                route = route,
                isSent = bestAscent != null,
                bestStyle = bestAscent?.style
            )
        }
        RouteListUiState(sector = sector, routes = routesWithStatus, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RouteListUiState()
    )

    init {
        viewModelScope.launch {
            _sector.value = areaRepository.getSectorById(sectorId)
        }
    }

    fun deleteRoute(route: Route) {
        viewModelScope.launch { routeRepository.deleteRoute(route) }
    }
}

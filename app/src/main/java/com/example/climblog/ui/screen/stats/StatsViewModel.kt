package com.example.climblog.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.WallRepository
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.SessionType
import com.example.climblog.domain.model.WallSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

enum class StatsFilter { VSE, SKALY, STENY }

data class WallStatsData(
    val totalSessions: Int = 0,
    val totalRoutes: Int = 0,
    val sessionsByType: Map<SessionType, Int> = emptyMap(),
    val topGrades: List<Pair<String, Int>> = emptyList(),   // grade → count, sorted desc
    val wallBreakdown: List<Pair<String, Int>> = emptyList() // wallName → sessionCount
)

data class StatsUiState(
    val filter: StatsFilter = StatsFilter.VSE,
    // Outdoor (Skály)
    val totalAscents: Int = 0,
    val sentRoutes: Int = 0,
    val ascentsByStyle: Map<AscentStyle, Int> = emptyMap(),
    // Indoor (Stěny)
    val wallStats: WallStatsData = WallStatsData(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val ascentRepository: AscentRepository,
    private val wallRepository: WallRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(StatsFilter.VSE)

    val uiState: StateFlow<StatsUiState> = combine(
        ascentRepository.getAllAscents(),
        wallRepository.getAllSessions(),
        _filter
    ) { ascents, sessions, filter ->
        val byStyle = AscentStyle.entries.associateWith { style ->
            ascents.count { it.style == style }
        }.filterValues { it > 0 }

        val sentRouteIds = ascents
            .filter { it.style != AscentStyle.ATTEMPT && it.style != AscentStyle.PROJECT }
            .map { it.routeId }.toSet()

        StatsUiState(
            filter = filter,
            totalAscents = ascents.size,
            sentRoutes = sentRouteIds.size,
            ascentsByStyle = byStyle,
            wallStats = buildWallStats(sessions),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    fun setFilter(filter: StatsFilter) { _filter.value = filter }

    private fun buildWallStats(sessions: List<WallSession>): WallStatsData {
        if (sessions.isEmpty()) return WallStatsData()
        val gradeAccumulator = mutableMapOf<String, Int>()
        sessions.forEach { session ->
            session.grades.forEach { entry ->
                gradeAccumulator[entry.grade] = (gradeAccumulator[entry.grade] ?: 0) + entry.count
            }
        }
        val topGrades = gradeAccumulator.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key to it.value }
        val wallBreakdown = sessions
            .groupBy { it.wallName }
            .map { (name, s) -> name to s.size }
            .sortedByDescending { it.second }
        return WallStatsData(
            totalSessions = sessions.size,
            totalRoutes = sessions.sumOf { it.totalRoutes },
            sessionsByType = SessionType.entries.associateWith { t -> sessions.count { it.type == t } },
            topGrades = topGrades,
            wallBreakdown = wallBreakdown
        )
    }
}

package com.example.climblog.ui.screen.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.local.dao.AscentWithGrade
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
    val topGrades: List<Pair<String, Int>> = emptyList(),
    val wallBreakdown: List<Pair<String, Int>> = emptyList()
)

data class GradePyramidRow(
    val grade: String,
    val onsight: Int = 0,
    val flash: Int = 0,
    val redpoint: Int = 0,
    val toprope: Int = 0,
) {
    val total: Int get() = onsight + flash + redpoint + toprope
}

data class StatsUiState(
    val filter: StatsFilter = StatsFilter.VSE,
    val totalAscents: Int = 0,
    val sentRoutes: Int = 0,
    val ascentsByStyle: Map<AscentStyle, Int> = emptyMap(),
    val pyramid: List<GradePyramidRow> = emptyList(), // hardest first
    val wallStats: WallStatsData = WallStatsData(),
    val isLoading: Boolean = true
)

private val GRADE_ORDER = listOf(
    "1", "2", "3", "3+",
    "4", "4+", "5", "5-", "5+", "5a", "5b", "5c", "5c+",
    "6-", "6", "6+", "6a", "6a+", "6b", "6b+", "6c", "6c+",
    "7-", "7", "7+", "7a", "7a+", "7b", "7b+", "7c", "7c+",
    "8-", "8", "8+", "8a", "8a+", "8b", "8b+", "8c", "8c+",
    "9a", "9a+", "9b", "9b+", "9c"
)

private fun gradeIndex(grade: String): Int {
    val idx = GRADE_ORDER.indexOf(grade.trim().lowercase())
    return if (idx >= 0) idx else GRADE_ORDER.size + grade.length
}

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val ascentRepository: AscentRepository,
    private val wallRepository: WallRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(StatsFilter.VSE)

    val uiState: StateFlow<StatsUiState> = combine(
        combine(
            ascentRepository.getAllAscents(),
            ascentRepository.getSentAscentsWithGrade(),
        ) { ascents, withGrade -> ascents to withGrade },
        combine(wallRepository.getAllSessions(), _filter) { sessions, filter -> sessions to filter }
    ) { (ascents, withGrade), (sessions, filter) ->
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
            pyramid = buildPyramid(withGrade),
            wallStats = buildWallStats(sessions),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
    )

    fun setFilter(filter: StatsFilter) { _filter.value = filter }

    private fun buildPyramid(ascents: List<AscentWithGrade>): List<GradePyramidRow> {
        if (ascents.isEmpty()) return emptyList()
        val acc = mutableMapOf<String, GradePyramidRow>()
        ascents.forEach { a ->
            val g = a.grade.trim()
            val row = acc.getOrDefault(g, GradePyramidRow(grade = g))
            acc[g] = when (a.style) {
                "ONSIGHT"  -> row.copy(onsight = row.onsight + 1)
                "FLASH"    -> row.copy(flash = row.flash + 1)
                "REDPOINT" -> row.copy(redpoint = row.redpoint + 1)
                "TOPROPE"  -> row.copy(toprope = row.toprope + 1)
                else       -> row
            }
        }
        return acc.values
            .filter { it.total > 0 }
            .sortedByDescending { gradeIndex(it.grade) }
    }

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

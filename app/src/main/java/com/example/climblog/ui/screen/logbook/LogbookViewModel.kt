package com.example.climblog.ui.screen.logbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.OutdoorSessionRepository
import com.example.climblog.data.repository.WallRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.OutdoorSession
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.WallSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class LogbookTab { SKALY, STENY }

data class LogbookEntry(
    val ascent: Ascent,
    val route: Route?
)

sealed class SkályFeedItem {
    data class Session(val session: OutdoorSession) : SkályFeedItem()
    data class SingleAscent(val entry: LogbookEntry) : SkályFeedItem()

    val date: Long get() = when (this) {
        is Session -> session.date
        is SingleAscent -> entry.ascent.date
    }
}

data class LogbookUiState(
    val skályFeed: List<SkályFeedItem> = emptyList(),
    val wallSessions: List<WallSession> = emptyList(),
    val selectedTab: LogbookTab = LogbookTab.SKALY,
    val isLoading: Boolean = true
)

@HiltViewModel
class LogbookViewModel @Inject constructor(
    private val ascentRepository: AscentRepository,
    private val wallRepository: WallRepository,
    private val outdoorSessionRepository: OutdoorSessionRepository,
    private val routeDao: RouteDao
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LogbookTab.SKALY)

    private val ascentsFlow = ascentRepository.getAllAscents().map { ascents ->
        ascents
            .filter { it.outdoorSessionId == null }
            .map { ascent ->
                LogbookEntry(ascent, routeDao.getRouteById(ascent.routeId)?.toDomain())
            }
    }

    val uiState: StateFlow<LogbookUiState> = combine(
        ascentsFlow,
        wallRepository.getAllSessions(),
        outdoorSessionRepository.getAllSessions(),
        _selectedTab
    ) { entries, wallSessions, outdoorSessions, tab ->
        val skályFeed = (
            entries.map { SkályFeedItem.SingleAscent(it) } +
            outdoorSessions.map { SkályFeedItem.Session(it) }
        ).sortedByDescending { it.date }

        LogbookUiState(
            skályFeed = skályFeed,
            wallSessions = wallSessions,
            selectedTab = tab,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LogbookUiState()
    )

    fun selectTab(tab: LogbookTab) { _selectedTab.value = tab }

    fun deleteWallSession(session: WallSession) {
        viewModelScope.launch { wallRepository.deleteSession(session) }
    }

    fun deleteOutdoorSession(session: OutdoorSession) {
        viewModelScope.launch { outdoorSessionRepository.deleteSession(session) }
    }
}

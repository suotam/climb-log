package com.example.climblog.ui.screen.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.Photo
import com.example.climblog.domain.model.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.util.Calendar
import javax.inject.Inject

data class CalendarDayEntry(
    val ascent: Ascent,
    val route: Route?
)

data class CalendarUiState(
    val displayedYear: Int,
    val displayedMonth: Int,
    val ascentsByDay: Map<Int, List<CalendarDayEntry>> = emptyMap(),
    val selectedDay: Int? = null,
    val dayPhotos: List<Photo> = emptyList(),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val ascentRepository: AscentRepository,
    private val routeRepository: RouteRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val today = Calendar.getInstance()
    private val _displayedMonth = MutableStateFlow(
        today.get(Calendar.YEAR) * 100 + today.get(Calendar.MONTH)
    )
    private val _selectedDay = MutableStateFlow<Int?>(null)

    private val _ascentsByDay: StateFlow<Map<Int, List<CalendarDayEntry>>> = combine(
        _displayedMonth,
        ascentRepository.getAllAscents()
    ) { monthKey, allAscents ->
        val year = monthKey / 100
        val month = monthKey % 100
        val (monthStart, monthEnd) = getMonthRange(year, month)
        val monthAscents = allAscents.filter { it.date in monthStart..monthEnd }
        val routeIds = monthAscents.map { it.routeId }.distinct()
        val routesById = routeRepository.getRoutesByIds(routeIds).associateBy { it.id }
        monthAscents.groupBy { ascent ->
            Calendar.getInstance().apply { timeInMillis = ascent.date }.get(Calendar.DAY_OF_MONTH)
        }.mapValues { (_, ascents) ->
            ascents.map { ascent -> CalendarDayEntry(ascent, routesById[ascent.routeId]) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyMap()
    )

    // Photos for the currently selected day
    private val _dayPhotos: StateFlow<List<Photo>> = combine(
        _displayedMonth,
        _selectedDay
    ) { monthKey, day -> monthKey to day }
        .flatMapLatest { (monthKey, day) ->
            if (day == null) return@flatMapLatest flowOf(emptyList())
            val year = monthKey / 100
            val month = monthKey % 100
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayStart = cal.timeInMillis
            cal.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = cal.timeInMillis - 1
            photoRepository.getPhotosByDay(dayStart, dayEnd)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val uiState: StateFlow<CalendarUiState> = combine(
        _displayedMonth,
        _selectedDay,
        _ascentsByDay,
        _dayPhotos
    ) { monthKey, selectedDay, ascentsByDay, dayPhotos ->
        CalendarUiState(
            displayedYear = monthKey / 100,
            displayedMonth = monthKey % 100,
            ascentsByDay = ascentsByDay,
            selectedDay = selectedDay,
            dayPhotos = dayPhotos,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            displayedYear = today.get(Calendar.YEAR),
            displayedMonth = today.get(Calendar.MONTH)
        )
    )

    fun previousMonth() {
        _displayedMonth.update { key ->
            val year = key / 100
            val month = key % 100
            if (month == 0) (year - 1) * 100 + 11 else year * 100 + (month - 1)
        }
        _selectedDay.value = null
    }

    fun nextMonth() {
        _displayedMonth.update { key ->
            val year = key / 100
            val month = key % 100
            if (month == 11) (year + 1) * 100 + 0 else year * 100 + (month + 1)
        }
        _selectedDay.value = null
    }

    fun selectDay(day: Int) {
        _selectedDay.value = if (_selectedDay.value == day) null else day
    }

    private fun getMonthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return start to (cal.timeInMillis - 1)
    }
}

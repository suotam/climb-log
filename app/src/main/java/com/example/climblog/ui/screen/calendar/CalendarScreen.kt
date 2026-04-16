package com.example.climblog.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.OutdoorSession
import com.example.climblog.ui.components.AscentStyleChip
import com.example.climblog.ui.components.PhotoSection
import java.util.Calendar

private val monthNames = arrayOf(
    "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
    "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
)
private val dayNames = arrayOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onAscentClick: (Long) -> Unit,
    onEditAscent: (routeId: Long, ascentId: Long) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Kalendář") })
        }
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                MonthHeader(
                    year = uiState.displayedYear,
                    month = uiState.displayedMonth,
                    onPrevious = viewModel::previousMonth,
                    onNext = viewModel::nextMonth
                )
            }

            item {
                MonthGrid(
                    year = uiState.displayedYear,
                    month = uiState.displayedMonth,
                    activityDays = uiState.activityDays,
                    selectedDay = uiState.selectedDay,
                    onDayClick = viewModel::selectDay
                )
            }

            // Selected day detail
            uiState.selectedDay?.let { day ->
                val standaloneEntries = uiState.standaloneAscentsByDay[day] ?: emptyList()
                val sessions = uiState.sessionsByDay[day] ?: emptyList()
                if (standaloneEntries.isNotEmpty() || sessions.isNotEmpty() || uiState.dayPhotos.isNotEmpty()) {
                    item {
                        Text(
                            "$day. ${monthNames[uiState.displayedMonth]} ${uiState.displayedYear}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Session cards
                    items(sessions, key = { "session_${it.id}" }) { session ->
                        CalendarSessionCard(
                            session = session,
                            onEditAscent = onEditAscent
                        )
                    }

                    // Standalone ascent cards
                    items(standaloneEntries, key = { it.ascent.id }) { entry ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAscentClick(entry.ascent.routeId) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AscentStyleChip(style = entry.ascent.style)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.route?.name ?: "Cesta #${entry.ascent.routeId}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (entry.route != null) {
                                        Text(
                                            text = entry.route.grade,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Photos for this day
                    if (uiState.dayPhotos.isNotEmpty()) {
                        item {
                            Text(
                                "Fotografie dne",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            PhotoSection(
                                photos = uiState.dayPhotos,
                                onPhotosAdded = {},   // read-only in calendar view
                                onDeleteSaved = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(year: Int, month: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Předchozí měsíc")
        }
        Text(
            text = "${monthNames[month]} $year",
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Následující měsíc")
        }
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    activityDays: Set<Int>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit
) {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val firstDayOfWeek = ((cal.get(Calendar.DAY_OF_WEEK) - 2 + 7) % 7)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val today = Calendar.getInstance()
    val isCurrentMonth = today.get(Calendar.YEAR) == year && today.get(Calendar.MONTH) == month
    val todayDay = if (isCurrentMonth) today.get(Calendar.DAY_OF_MONTH) else -1

    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            dayNames.forEach { name ->
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        val totalCells = firstDayOfWeek + daysInMonth
        val rows = (totalCells + 6) / 7
        var dayCounter = 1

        repeat(rows) { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    if (cellIndex < firstDayOfWeek || dayCounter > daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val day = dayCounter
                        DayCell(
                            day = day,
                            isToday = day == todayDay,
                            hasAscent = day in activityDays,
                            isSelected = day == selectedDay,
                            onClick = { onDayClick(day) },
                            modifier = Modifier.weight(1f)
                        )
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarSessionCard(
    session: OutdoorSession,
    onEditAscent: (routeId: Long, ascentId: Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Terrain,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(session.cragDisplayName, style = MaterialTheme.typography.titleSmall)
                if (session.routes.isNotEmpty()) {
                    Text(
                        "· ${session.routes.size} ${when (session.routes.size) {
                            1 -> "cesta"; in 2..4 -> "cesty"; else -> "cest"
                        }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (session.routes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ) {
                    Column {
                        session.routes.forEach { route ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEditAscent(route.routeId, route.ascentId) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AscentStyleChip(style = route.style)
                                Text(
                                    route.routeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    route.grade,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
            if (session.notes.isNotBlank()) {
                Text(
                    session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    isToday: Boolean,
    hasAscent: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            if (hasAscent) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

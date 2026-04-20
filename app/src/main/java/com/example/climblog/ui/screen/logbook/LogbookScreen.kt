package com.example.climblog.ui.screen.logbook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.OutdoorSession
import com.example.climblog.domain.model.WallSession
import com.example.climblog.ui.components.AscentStyleChip
import com.example.climblog.ui.components.EmptyState
import com.example.climblog.ui.components.GradeChip
import com.example.climblog.ui.components.SettingsIconButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale("cs"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookScreen(
    onRouteClick: (Long) -> Unit,
    onEditAscent: (routeId: Long, ascentId: Long) -> Unit,
    onSessionClick: (sessionId: Long) -> Unit,
    onAddWallSession: () -> Unit,
    onAddOutdoorSession: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: LogbookViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteWallTarget by remember { mutableStateOf<WallSession?>(null) }
    var deleteOutdoorTarget by remember { mutableStateOf<OutdoorSession?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deník") },
                actions = { SettingsIconButton(onSettingsClick) }
            )
        },
        floatingActionButton = {
            when (uiState.selectedTab) {
                LogbookTab.SKALY -> FloatingActionButton(onClick = onAddOutdoorSession) {
                    Icon(Icons.Filled.Add, contentDescription = "Přidat session")
                }
                LogbookTab.STENY -> FloatingActionButton(onClick = onAddWallSession) {
                    Icon(Icons.Filled.Add, contentDescription = "Přidat trénink")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
                LogbookTab.entries.forEach { tab ->
                    Tab(
                        selected = uiState.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(when (tab) {
                                LogbookTab.SKALY -> "Skály"
                                LogbookTab.STENY -> "Stěny"
                            })
                        }
                    )
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            when (uiState.selectedTab) {
                LogbookTab.SKALY -> OutdoorTab(
                    feed = uiState.skályFeed,
                    onRouteClick = onRouteClick,
                    onEditAscent = onEditAscent,
                    onSessionClick = onSessionClick,
                    onDeleteSession = { deleteOutdoorTarget = it }
                )
                LogbookTab.STENY -> WallTab(
                    sessions = uiState.wallSessions,
                    onDeleteSession = { deleteWallTarget = it }
                )
            }
        }
    }

    deleteWallTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteWallTarget = null },
            title = { Text("Smazat trénink?") },
            text = { Text("${session.wallName} · ${dateFormat.format(Date(session.date))}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWallSession(session)
                    deleteWallTarget = null
                }) { Text("Smazat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteWallTarget = null }) { Text("Zrušit") }
            }
        )
    }

    deleteOutdoorTarget?.let { session ->
        AlertDialog(
            onDismissRequest = { deleteOutdoorTarget = null },
            title = { Text("Smazat session?") },
            text = { Text("${session.cragDisplayName} · ${dateFormat.format(Date(session.date))}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteOutdoorSession(session)
                    deleteOutdoorTarget = null
                }) { Text("Smazat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteOutdoorTarget = null }) { Text("Zrušit") }
            }
        )
    }
}

// ── Outdoor tab ───────────────────────────────────────────────────────────────

@Composable
private fun OutdoorTab(
    feed: List<SkályFeedItem>,
    onRouteClick: (Long) -> Unit,
    onEditAscent: (routeId: Long, ascentId: Long) -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (OutdoorSession) -> Unit
) {
    if (feed.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Deník je prázdný",
            subtitle = "Stiskni + a přidej svou první session, nebo zaznamenej přelez na detailu cesty."
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(feed, key = { when (it) {
            is SkályFeedItem.Session -> "session_${it.session.id}"
            is SkályFeedItem.SingleAscent -> "ascent_${it.entry.ascent.id}"
        }}) { item ->
            when (item) {
                is SkályFeedItem.Session -> OutdoorSessionCard(
                    session = item.session,
                    onClick = { onSessionClick(item.session.id) },
                    onEditAscent = onEditAscent,
                    onDelete = { onDeleteSession(item.session) }
                )
                is SkályFeedItem.SingleAscent -> OutdoorEntryCard(
                    entry = item.entry,
                    onClick = item.entry.route?.let { { onRouteClick(it.id) } }
                )
            }
        }
    }
}

@Composable
private fun OutdoorSessionCard(
    session: OutdoorSession,
    onClick: () -> Unit,
    onEditAscent: (routeId: Long, ascentId: Long) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    Text(session.cragDisplayName, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    dateFormat.format(Date(session.date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.routes.isNotEmpty()) {
                    Text(
                        "${session.routes.size} ${pluralCesty(session.routes.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Smazat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun pluralCesty(count: Int) = when (count) {
    1 -> "cesta"
    in 2..4 -> "cesty"
    else -> "cest"
}

@Composable
private fun OutdoorEntryCard(entry: LogbookEntry, onClick: (() -> Unit)?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.route?.name ?: "Neznámá cesta",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AscentStyleChip(style = entry.ascent.style)
                    Text(
                        text = dateFormat.format(Date(entry.ascent.date)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.ascent.attempts > 1) {
                        Text(
                            "${entry.ascent.attempts}×",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (entry.ascent.personalNote.isNotBlank()) {
                    Text(
                        text = entry.ascent.personalNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            entry.route?.let { GradeChip(grade = it.grade) }
        }
    }
}

// ── Wall tab ──────────────────────────────────────────────────────────────────

@Composable
private fun WallTab(
    sessions: List<WallSession>,
    onDeleteSession: (WallSession) -> Unit
) {
    if (sessions.isEmpty()) {
        EmptyState(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            title = "Žádné tréninky",
            subtitle = "Stiskni + a zaznamenej svůj první trénink na stěně."
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sessions, key = { it.id }) { session ->
            WallSessionCard(session = session, onDelete = { onDeleteSession(session) })
        }
    }
}

@Composable
private fun WallSessionCard(session: WallSession, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(session.wallName, style = MaterialTheme.typography.titleMedium)
                    AssistChip(
                        onClick = {},
                        label = { Text(session.type.label, style = MaterialTheme.typography.labelSmall) }
                    )
                }
                Text(
                    dateFormat.format(Date(session.date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.totalRoutes > 0) {
                    Text(
                        "${session.totalRoutes} cest · ${session.gradeSummary}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (session.notes.isNotBlank()) {
                    Text(
                        session.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Smazat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

package com.example.climblog.ui.screen.wishlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.WishlistEntry
import com.example.climblog.ui.components.EmptyState
import com.example.climblog.ui.components.GradeChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onRouteClick: (Long) -> Unit,
    viewModel: WishlistViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var confirmRemove by remember { mutableStateOf<WishlistEntry?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Cíle") })
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.entries.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.BookmarkBorder,
                title = "Žádné cíle",
                subtitle = "Přidej cesty z jejich detailu pomocí ikony záložky"
            )
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.entries, key = { it.id }) { entry ->
                WishlistCard(
                    entry = entry,
                    onClick = { onRouteClick(entry.routeId) },
                    onRemove = { confirmRemove = entry }
                )
            }
        }
    }

    confirmRemove?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Odebrat z cílů?") },
            text = { Text(entry.route?.name ?: "Tato cesta") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(entry)
                    confirmRemove = null
                }) { Text("Odebrat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = null }) { Text("Zrušit") }
            }
        )
    }
}

@Composable
private fun WishlistCard(
    entry: WishlistEntry,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    entry.route?.let { GradeChip(grade = it.grade) }
                    Text(
                        text = entry.route?.name ?: "Cesta #${entry.routeId}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                PriorityIndicator(priority = entry.priority)
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Odebrat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun PriorityIndicator(priority: Int, modifier: Modifier = Modifier) {
    val label = when (priority) {
        3 -> "Vysoká priorita"
        1 -> "Nízká priorita"
        else -> "Střední priorita"
    }
    val color = when (priority) {
        3 -> MaterialTheme.colorScheme.error
        1 -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.primary
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(priority) {
            Icon(Icons.Filled.Bookmark, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

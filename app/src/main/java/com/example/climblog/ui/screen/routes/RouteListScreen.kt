package com.example.climblog.ui.screen.routes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.Route
import com.example.climblog.ui.components.AscentStyleChip
import com.example.climblog.ui.components.GradeChip
import com.example.climblog.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteListScreen(
    sectorId: Long,
    onRouteClick: (Long) -> Unit,
    onAddRoute: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: RouteListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteTarget by remember { mutableStateOf<Route?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.sector?.name ?: "Cesty") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRoute) {
                Icon(Icons.Filled.Add, contentDescription = "Přidat cestu")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.routes.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.RadioButtonUnchecked,
                title = "Žádné cesty",
                subtitle = "Tento sektor zatím neobsahuje žádné cesty.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.routes, key = { it.route.id }) { item ->
                    RouteListCard(
                        item = item,
                        onClick = { onRouteClick(item.route.id) },
                        onDelete = { deleteTarget = item.route }
                    )
                }
            }
        }
    }

    deleteTarget?.let { route ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Smazat cestu?") },
            text = { Text("\"${route.name}\" bude trvale smazána včetně všech přelezů.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRoute(route)
                    deleteTarget = null
                }) { Text("Smazat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Zrušit") }
            }
        )
    }
}

@Composable
private fun RouteListCard(item: RouteWithStatus, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isSent) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (item.isSent) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.route.name, style = MaterialTheme.typography.titleMedium)
                item.route.description.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GradeChip(grade = item.route.grade)
                item.bestStyle?.let { style -> AscentStyleChip(style = style) }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete, contentDescription = "Smazat cestu",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

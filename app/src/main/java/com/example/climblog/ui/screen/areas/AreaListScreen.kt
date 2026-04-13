package com.example.climblog.ui.screen.areas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.Area
import com.example.climblog.ui.components.EmptyState
import com.example.climblog.ui.components.OsmMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaListScreen(
    onAreaClick: (Long) -> Unit,
    viewModel: AreaListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ClimbLog") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = viewModel::toggleMapView) {
                        Icon(
                            imageVector = if (uiState.showMap) Icons.AutoMirrored.Filled.List else Icons.Filled.Map,
                            contentDescription = if (uiState.showMap) "Přepnout na seznam" else "Přepnout na mapu"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.showMap) {
            MapContent(
                uiState = uiState,
                padding = padding,
                onMarkerClick = viewModel::selectAreaOnMap,
                onAreaNavigate = onAreaClick
            )
        } else {
            ListContent(
                uiState = uiState,
                padding = padding,
                onSearchChange = viewModel::onSearchQueryChange,
                onAreaClick = onAreaClick
            )
        }
    }
}

// ── List mode ──────────────────────────────────────────────────────────────────

@Composable
private fun ListContent(
    uiState: AreaListUiState,
    padding: PaddingValues,
    onSearchChange: (String) -> Unit,
    onAreaClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Hledat oblasti…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.areas.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Landscape,
                title = "Žádné oblasti",
                subtitle = if (uiState.searchQuery.isBlank())
                    "Databáze oblastí se načítá…"
                else
                    "Pro \"${uiState.searchQuery}\" nebyla nalezena žádná oblast."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.areas, key = { it.id }) { area ->
                    AreaCard(area = area, onClick = { onAreaClick(area.id) })
                }
            }
        }
    }
}

// ── Map mode ───────────────────────────────────────────────────────────────────

@Composable
private fun MapContent(
    uiState: AreaListUiState,
    padding: PaddingValues,
    onMarkerClick: (Long) -> Unit,
    onAreaNavigate: (Long) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        OsmMapView(
            areas = uiState.allAreas,
            onMarkerClick = onMarkerClick,
            selectedAreaId = uiState.selectedAreaId,
            modifier = Modifier.fillMaxSize()
        )

        // Selected area card at the bottom
        val selectedArea = uiState.allAreas.find { it.id == uiState.selectedAreaId }
        if (selectedArea != null) {
            AreaMapCard(
                area = selectedArea,
                onNavigate = { onAreaNavigate(selectedArea.id) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun AreaMapCard(
    area: Area,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.Landscape,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(area.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${area.region} · ${area.rockType}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onNavigate) {
                Text("Otevřít")
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                    modifier = Modifier.size(16.dp))
            }
        }
    }
}

// ── Area list card ─────────────────────────────────────────────────────────────

@Composable
private fun AreaCard(area: Area, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Landscape,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = area.name,
                    style = MaterialTheme.typography.titleMedium
                )
                if (area.region.isNotBlank()) {
                    Text(
                        text = "${area.region} · ${area.country}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (area.rockType.isNotBlank()) {
                    Text(
                        text = area.rockType,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

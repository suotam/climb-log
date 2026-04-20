package com.example.climblog.ui.screen.areas

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Landscape
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
import com.example.climblog.ui.components.GradeChip
import com.example.climblog.ui.components.OsmMapView
import com.example.climblog.ui.components.SettingsIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaListScreen(
    onAreaClick: (Long) -> Unit,
    onSectorClick: (sectorId: Long) -> Unit,
    onRouteClick: (routeId: Long) -> Unit,
    onSettingsClick: () -> Unit = {},
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
                    SettingsIconButton(onSettingsClick)
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
                onModeChange = viewModel::onSearchModeChange,
                onAreaClick = onAreaClick,
                onSectorClick = onSectorClick,
                onRouteClick = onRouteClick
            )
        }
    }
}

// ── List mode ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListContent(
    uiState: AreaListUiState,
    padding: PaddingValues,
    onSearchChange: (String) -> Unit,
    onModeChange: (SearchMode) -> Unit,
    onAreaClick: (Long) -> Unit,
    onSectorClick: (Long) -> Unit,
    onRouteClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // ── Search field ──────────────────────────────────────────────────────
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
                Text(when (uiState.searchMode) {
                    SearchMode.AREAS -> "Hledat oblasti…"
                    SearchMode.SECTORS -> "Hledat sektory…"
                    SearchMode.ROUTES -> "Hledat cesty…"
                })
            },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ── Mode selector ─────────────────────────────────────────────────────
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
        ) {
            SearchMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = uiState.searchMode == mode,
                    onClick = { onModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, SearchMode.entries.size),
                    label = {
                        Text(when (mode) {
                            SearchMode.AREAS -> "Oblasti"
                            SearchMode.SECTORS -> "Sektory"
                            SearchMode.ROUTES -> "Cesty"
                        }, style = MaterialTheme.typography.labelMedium)
                    }
                )
            }
        }

        // ── Results ───────────────────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        when (uiState.searchMode) {
            SearchMode.AREAS -> AreaResults(
                areas = uiState.areas,
                query = uiState.searchQuery,
                onAreaClick = onAreaClick
            )
            SearchMode.SECTORS -> SectorResults(
                results = uiState.sectorResults,
                query = uiState.searchQuery,
                onSectorClick = onSectorClick
            )
            SearchMode.ROUTES -> RouteResults(
                results = uiState.routeResults,
                query = uiState.searchQuery,
                onRouteClick = onRouteClick
            )
        }
    }
}

@Composable
private fun AreaResults(areas: List<Area>, query: String, onAreaClick: (Long) -> Unit) {
    if (areas.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Landscape,
            title = "Žádné oblasti",
            subtitle = if (query.isBlank()) "Databáze oblastí se načítá…"
                       else "Pro \"$query\" nebyla nalezena žádná oblast."
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(areas, key = { it.id }) { area ->
                AreaCard(area = area, onClick = { onAreaClick(area.id) })
            }
        }
    }
}

@Composable
private fun SectorResults(
    results: List<com.example.climblog.data.local.dao.SectorWithArea>,
    query: String,
    onSectorClick: (Long) -> Unit
) {
    if (results.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Landscape,
            title = if (query.length < 2) "Zadejte alespoň 2 znaky" else "Žádné sektory",
            subtitle = if (query.length >= 2) "Pro \"$query\" nebyl nalezen žádný sektor." else ""
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.sectorId }) { result ->
                ResultCard(
                    title = result.sectorName,
                    subtitle = result.areaName,
                    onClick = { onSectorClick(result.sectorId) }
                )
            }
        }
    }
}

@Composable
private fun RouteResults(
    results: List<com.example.climblog.data.local.dao.RouteWithContext>,
    query: String,
    onRouteClick: (Long) -> Unit
) {
    if (results.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.Landscape,
            title = if (query.length < 2) "Zadejte alespoň 2 znaky" else "Žádné cesty",
            subtitle = if (query.length >= 2) "Pro \"$query\" nebyla nalezena žádná cesta." else ""
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(results, key = { it.routeId }) { result ->
                RouteResultCard(result = result, onClick = { onRouteClick(result.routeId) })
            }
        }
    }
}

@Composable
private fun ResultCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RouteResultCard(
    result: com.example.climblog.data.local.dao.RouteWithContext,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GradeChip(grade = result.grade)
            Column(modifier = Modifier.weight(1f)) {
                Text(result.routeName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${result.areaName} · ${result.sectorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                Text(text = area.name, style = MaterialTheme.typography.titleMedium)
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

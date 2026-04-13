package com.example.climblog.ui.screen.sectors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.Sector
import com.example.climblog.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SectorListScreen(
    areaId: Long,
    onSectorClick: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: SectorListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.area?.name ?: "Sektory") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.sectors.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Terrain,
                title = "Žádné sektory",
                subtitle = "Tato oblast zatím neobsahuje žádné sektory.",
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.sectors, key = { it.id }) { sector ->
                    SectorCard(sector = sector, onClick = { onSectorClick(sector.id) })
                }
            }
        }
    }
}

@Composable
private fun SectorCard(sector: Sector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = sector.name, style = MaterialTheme.typography.titleMedium)
            if (sector.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = sector.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            if (sector.approach.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Přístup: ${sector.approach}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

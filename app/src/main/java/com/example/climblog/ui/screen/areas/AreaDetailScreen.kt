package com.example.climblog.ui.screen.areas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.ui.components.OsmMapView
import com.example.climblog.ui.components.PhotoSection
import com.example.climblog.ui.components.SettingsIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AreaDetailScreen(
    areaId: Long,
    onSectorsClick: (Long) -> Unit,
    onNavigateUp: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: AreaDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.area?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = { SettingsIconButton(onSettingsClick) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val area = uiState.area ?: return@Scaffold

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Area info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.Landscape,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            if (area.region.isNotBlank()) {
                                Text(
                                    "${area.region} · ${area.country}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (area.rockType.isNotBlank()) {
                                Text(
                                    area.rockType,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    if (area.description.isNotBlank()) {
                        Text(
                            area.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Mini map (only if area has coordinates)
            if (area.latitude != null && area.longitude != null) {
                Text("Poloha", style = MaterialTheme.typography.titleMedium)
                OsmMapView(
                    areas = listOf(area),
                    onMarkerClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            // Photos
            Text(
                "Fotografie oblasti",
                style = MaterialTheme.typography.titleMedium
            )
            PhotoSection(
                photos = uiState.photos,
                onPhotosAdded = viewModel::addPhotos,
                onDeleteSaved = viewModel::deletePhoto
            )

            Spacer(Modifier.height(8.dp))

            // Navigate to sectors
            Button(
                onClick = { onSectorsClick(areaId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Zobrazit sektory", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

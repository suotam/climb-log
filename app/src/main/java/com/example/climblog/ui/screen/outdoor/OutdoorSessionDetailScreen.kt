package com.example.climblog.ui.screen.outdoor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.OutdoorSessionRoute
import com.example.climblog.ui.components.AscentStyleChip
import com.example.climblog.ui.components.PhotoSection
import com.example.climblog.ui.components.SettingsIconButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale("cs"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutdoorSessionDetailScreen(
    onNavigateUp: () -> Unit,
    onEditAscent: (routeId: Long, ascentId: Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: OutdoorSessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val session = uiState.session

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.cragDisplayName ?: "Session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Uložit")
                        }
                    }
                    SettingsIconButton(onSettingsClick)
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Header info ───────────────────────────────────────────────────
            session?.let {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(it.cragDisplayName, style = MaterialTheme.typography.titleLarge)
                        Text(
                            dateFormat.format(Date(it.date)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // ── Notes ─────────────────────────────────────────────────────────
            SectionLabel("Poznámka")
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                placeholder = { Text("Jak šlo lezení, podmínky…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6
            )

            // ── Photos ────────────────────────────────────────────────────────
            SectionLabel("Fotky")
            PhotoSection(
                photos = uiState.photos,
                pendingUris = uiState.pendingPhotoUris,
                onPhotosAdded = viewModel::addPhotos,
                onDeleteSaved = viewModel::deleteExistingPhoto,
                onDeletePending = viewModel::removePendingPhoto
            )

            // ── Routes ────────────────────────────────────────────────────────
            if (session != null && session.routes.isNotEmpty()) {
                HorizontalDivider()
                SectionLabel("Cesty (${session.routes.size})")
                RoutesList(
                    routes = session.routes,
                    onRouteClick = onEditAscent
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RoutesList(
    routes: List<OutdoorSessionRoute>,
    onRouteClick: (routeId: Long, ascentId: Long) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            routes.forEachIndexed { index, route ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRouteClick(route.routeId, route.ascentId) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AscentStyleChip(style = route.style)
                    Text(
                        route.routeName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            route.grade,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (index < routes.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

package com.example.climblog.ui.screen.outdoor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.Area
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.Photo
import com.example.climblog.ui.components.PhotoSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale("cs"))

private val styleOptions = listOf(
    AscentStyle.ONSIGHT to "OS",
    AscentStyle.FLASH to "FL",
    AscentStyle.REDPOINT to "RP",
    AscentStyle.TOPROPE to "TR",
    AscentStyle.ATTEMPT to "P"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOutdoorSessionScreen(
    onSaved: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: AddOutdoorSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nová session") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = uiState.canSave && !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Uložit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Typ skály ─────────────────────────────────────────────────────
            SectionLabel("Typ lokality")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                CragMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = uiState.mode == mode,
                        onClick = { viewModel.onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, CragMode.entries.size),
                        label = {
                            Text(
                                when (mode) {
                                    CragMode.KNOWN_AREA -> "Oblast z databáze"
                                    CragMode.CUSTOM_CRAG -> "Vlastní skála"
                                }
                            )
                        }
                    )
                }
            }

            HorizontalDivider()

            when (uiState.mode) {
                CragMode.KNOWN_AREA -> KnownAreaSection(
                    uiState = uiState,
                    onAreaSearchChange = viewModel::onAreaSearchChange,
                    onSelectArea = viewModel::selectArea,
                    onToggleRoute = viewModel::toggleRoute,
                    onSetRouteStyle = viewModel::setRouteStyle
                )
                CragMode.CUSTOM_CRAG -> CustomCragSection(
                    cragName = uiState.customCragName,
                    onCragNameChange = viewModel::onCustomCragNameChange
                )
            }

            HorizontalDivider()

            // ── Datum ─────────────────────────────────────────────────────────
            SectionLabel("Datum")
            var showDatePicker by remember { mutableStateOf(false) }
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(dateFormat.format(Date(uiState.date)))
            }
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                            showDatePicker = false
                        }) { Text("OK") }
                    }
                ) { DatePicker(state = datePickerState) }
            }

            // ── Fotky ─────────────────────────────────────────────────────────
            SectionLabel("Fotky (volitelné)")
            PhotoSection(
                photos = emptyList<Photo>(),
                pendingUris = uiState.pendingPhotoUris,
                onPhotosAdded = viewModel::addPhotos,
                onDeletePending = viewModel::removePhoto
            )

            // ── Poznámka ──────────────────────────────────────────────────────
            SectionLabel("Poznámka (volitelné)")
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                placeholder = { Text("Jak šlo lezení, podmínky…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Known area section ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KnownAreaSection(
    uiState: AddOutdoorSessionUiState,
    onAreaSearchChange: (String) -> Unit,
    onSelectArea: (Area) -> Unit,
    onToggleRoute: (sectorId: Long, routeId: Long) -> Unit,
    onSetRouteStyle: (sectorId: Long, routeId: Long, AscentStyle) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }

    SectionLabel("Oblast")

    ExposedDropdownMenuBox(
        expanded = showDropdown && uiState.filteredAreas.isNotEmpty(),
        onExpandedChange = { showDropdown = it }
    ) {
        OutlinedTextField(
            value = uiState.areaSearch,
            onValueChange = {
                onAreaSearchChange(it)
                showDropdown = true
            },
            placeholder = { Text("Hledat oblast…") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            singleLine = true,
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = showDropdown && uiState.filteredAreas.isNotEmpty(),
            onDismissRequest = { showDropdown = false }
        ) {
            uiState.filteredAreas.take(20).forEach { area ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(area.name, style = MaterialTheme.typography.bodyMedium)
                            if (area.region.isNotBlank()) {
                                Text(
                                    area.region,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectArea(area)
                        showDropdown = false
                    }
                )
            }
        }
    }

    if (uiState.selectedArea != null) {
        if (uiState.isLoadingRoutes) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (uiState.sectorsWithRoutes.isEmpty()) {
            Text(
                "Tato oblast nemá žádné cesty v databázi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val selectedCount = uiState.selectedRoutes.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Cesty")
                if (selectedCount > 0) {
                    Text(
                        "vybráno $selectedCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            uiState.sectorsWithRoutes.forEach { sector ->
                SectorRouteGroup(
                    sector = sector,
                    onToggleRoute = { routeId -> onToggleRoute(sector.sectorId, routeId) },
                    onSetStyle = { routeId, style -> onSetRouteStyle(sector.sectorId, routeId, style) }
                )
            }
        }
    }
}

@Composable
private fun SectorRouteGroup(
    sector: SectorWithRoutes,
    onToggleRoute: (Long) -> Unit,
    onSetStyle: (Long, AscentStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val selectedCount = sector.routes.count { it.isSelected }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Sector header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    sector.sectorName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                if (selectedCount > 0) {
                    Text(
                        "$selectedCount",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    sector.routes.forEach { route ->
                        RouteSelectionRow(
                            route = route,
                            onToggle = { onToggleRoute(route.routeId) },
                            onSetStyle = { style -> onSetStyle(route.routeId, style) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteSelectionRow(
    route: SelectableRoute,
    onToggle: () -> Unit,
    onSetStyle: (AscentStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = route.isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                route.routeName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
            ) {
                Text(
                    route.grade,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (route.isSelected) {
            Spacer(Modifier.height(6.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.padding(start = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                styleOptions.forEach { (style, label) ->
                    val isSelected = route.style == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSetStyle(style) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        thickness = 0.5.dp
    )
}

// ── Custom crag section ───────────────────────────────────────────────────────

@Composable
private fun CustomCragSection(
    cragName: String,
    onCragNameChange: (String) -> Unit
) {
    SectionLabel("Jméno skály")
    OutlinedTextField(
        value = cragName,
        onValueChange = onCragNameChange,
        placeholder = { Text("Název skály nebo lokality") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Text(
        "Cesty není třeba zadávat — stačí jméno, datum a případné fotky.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

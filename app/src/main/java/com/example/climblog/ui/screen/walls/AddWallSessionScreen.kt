package com.example.climblog.ui.screen.walls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.BoulderGradeSystem
import com.example.climblog.domain.model.RopeGradeSystem
import com.example.climblog.domain.model.SessionType
import com.example.climblog.domain.model.Wall
import com.example.climblog.ui.components.WallMapView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale("cs"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWallSessionScreen(
    onSaved: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: AddWallSessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nový trénink") },
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

            // ── Stěna ─────────────────────────────────────────────────────────
            SectionLabel("Stěna")
            WallSelector(
                walls = uiState.walls,
                selectedWall = uiState.selectedWall,
                showNewWallField = uiState.showNewWallField,
                newWallName = uiState.newWallName,
                onSelectWall = viewModel::selectWall,
                onDeleteWall = viewModel::deleteWall,
                onToggleNewWall = viewModel::toggleNewWallField,
                onNewWallNameChange = viewModel::onNewWallNameChange,
                onSaveNewWall = viewModel::saveNewWall
            )

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

            // ── Typ ───────────────────────────────────────────────────────────
            SectionLabel("Typ lezení")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SessionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = uiState.type == type,
                        onClick = { viewModel.onTypeChange(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, SessionType.entries.size),
                        label = { Text(type.label) }
                    )
                }
            }

            // ── Grade systém ──────────────────────────────────────────────────
            if (uiState.type == SessionType.ROPE) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RopeGradeSystem.entries.forEachIndexed { index, system ->
                        SegmentedButton(
                            selected = uiState.ropeGradeSystem == system,
                            onClick = { viewModel.onRopeGradeSystemChange(system) },
                            shape = SegmentedButtonDefaults.itemShape(index, RopeGradeSystem.entries.size),
                            label = { Text(system.label) }
                        )
                    }
                }
            } else {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    BoulderGradeSystem.entries.forEachIndexed { index, system ->
                        SegmentedButton(
                            selected = uiState.boulderGradeSystem == system,
                            onClick = { viewModel.onBoulderGradeSystemChange(system) },
                            shape = SegmentedButtonDefaults.itemShape(index, BoulderGradeSystem.entries.size),
                            label = { Text(system.label) }
                        )
                    }
                }
            }

            // ── Grade picker ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionLabel("Cesty")
                if (uiState.totalRoutes > 0) {
                    Text(
                        "celkem ${uiState.totalRoutes}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                "Klepnutím přidej · Dlouhé klepnutí = uber o 1",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            GradePicker(
                grades = uiState.currentGrades,
                counts = uiState.grades,
                onIncrement = viewModel::incrementGrade,
                onDecrement = viewModel::decrementGrade
            )

            // ── Poznámka ──────────────────────────────────────────────────────
            SectionLabel("Poznámka (volitelné)")
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChange,
                placeholder = { Text("Jak šlo lezení, co trénovat…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Wall selector ──────────────────────────────────────────────────────────────

@Composable
private fun WallSelector(
    walls: List<Wall>,
    selectedWall: Wall?,
    showNewWallField: Boolean,
    newWallName: String,
    onSelectWall: (Wall) -> Unit,
    onDeleteWall: (Wall) -> Unit,
    onToggleNewWall: () -> Unit,
    onNewWallNameChange: (String) -> Unit,
    onSaveNewWall: () -> Unit
) {
    val wallsOnMap = remember(walls) { walls.filter { it.latitude != null && it.longitude != null } }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // ── Mapa preset stěn ───────────────────────────────────────────────
        if (wallsOnMap.isNotEmpty()) {
            WallMapView(
                walls = wallsOnMap,
                selectedWallId = selectedWall?.id,
                onWallClick = onSelectWall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
        }

        // ── Chipsy všech stěn ──────────────────────────────────────────────
        if (walls.isEmpty() && !showNewWallField) {
            Text(
                "Žádné stěny — přidej svou první",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (walls.isNotEmpty()) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                walls.forEach { wall ->
                    val isSelected = selectedWall?.id == wall.id
                    InputChip(
                        selected = isSelected,
                        onClick = { onSelectWall(wall) },
                        label = { Text(wall.name) },
                        trailingIcon = if (isSelected) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        avatar = null
                    )
                }
            }
        }

        if (showNewWallField) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newWallName,
                    onValueChange = onNewWallNameChange,
                    placeholder = { Text("Název stěny") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                FilledTonalButton(
                    onClick = onSaveNewWall,
                    enabled = newWallName.isNotBlank()
                ) { Text("Přidat") }
            }
        }

        TextButton(
            onClick = onToggleNewWall,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                if (showNewWallField) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(if (showNewWallField) "Zrušit" else "Přidat vlastní stěnu")
        }
    }
}

// ── Grade picker ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun GradePicker(
    grades: List<String>,
    counts: Map<String, Int>,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grades.forEach { grade ->
            val count = counts[grade] ?: 0
            GradeCountChip(
                grade = grade,
                count = count,
                onIncrement = { onIncrement(grade) },
                onDecrement = { onDecrement(grade) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GradeCountChip(
    grade: String,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isActive = count > 0
    val colors = if (isActive) {
        Pair(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer)
    } else {
        Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.first,
        contentColor = colors.second,
        border = if (!isActive) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.combinedClickable(
            onClick = onIncrement,
            onLongClick = onDecrement
        )
    ) {
        Text(
            text = if (isActive) "$grade ×$count" else grade,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
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

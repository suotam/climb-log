package com.example.climblog.ui.screen.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.GradeSystem
import com.example.climblog.domain.model.RouteType
import com.example.climblog.ui.components.SettingsIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRouteScreen(
    onSaved: () -> Unit,
    onNavigateUp: () -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: EditRouteViewModel = hiltViewModel()
) {
    val s by viewModel.uiState.collectAsState()

    LaunchedEffect(s.isSaved) {
        if (s.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.isEditMode) "Upravit cestu" else "Nová cesta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = s.canSave && !s.isSaving
                    ) {
                        if (s.isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("Uložit")
                    }
                    SettingsIconButton(onSettingsClick)
                }
            )
        }
    ) { padding ->
        if (s.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Název ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = s.name,
                onValueChange = viewModel::onNameChange,
                label = { Text("Název *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Obtížnost + Systém ────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = s.grade,
                    onValueChange = viewModel::onGradeChange,
                    label = { Text("Obtížnost *") },
                    singleLine = true,
                    placeholder = { Text("6a+") },
                    modifier = Modifier.width(110.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Systém",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        GradeSystem.entries.forEachIndexed { i, gs ->
                            SegmentedButton(
                                selected = s.gradeSystem == gs,
                                onClick = { viewModel.onGradeSystemChange(gs) },
                                shape = SegmentedButtonDefaults.itemShape(i, GradeSystem.entries.size),
                                label = { Text(gs.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // ── Typ ───────────────────────────────────────────────────────────
            Column {
                Text(
                    "Typ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    RouteType.entries.forEachIndexed { i, type ->
                        SegmentedButton(
                            selected = s.type == type,
                            onClick = { viewModel.onTypeChange(type) },
                            shape = SegmentedButtonDefaults.itemShape(i, RouteType.entries.size),
                            label = { Text(type.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }

            // ── Délka + Bolty ─────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = s.lengthText,
                    onValueChange = viewModel::onLengthChange,
                    label = { Text("Délka (m)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = s.boltsText,
                    onValueChange = viewModel::onBoltsChange,
                    label = { Text("Boltů") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // ── Popis ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = s.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Popis") },
                placeholder = { Text("Charakter cesty, beta, podmínky…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 5
            )

            // ── Prvovýstup ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = s.firstAscent,
                    onValueChange = viewModel::onFirstAscentChange,
                    label = { Text("Prvovýstup") },
                    singleLine = true,
                    placeholder = { Text("Jméno lezce") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = s.firstAscentYearText,
                    onValueChange = viewModel::onFirstAscentYearChange,
                    label = { Text("Rok") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(90.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

package com.example.climblog.ui.screen.ascent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.ui.components.GradeChip
import com.example.climblog.ui.components.PhotoSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale("cs"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogAscentScreen(
    routeId: Long,
    editAscentId: Long?,
    onSaved: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: LogAscentViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editAscentId != null) "Upravit přelez" else "Zaznamenat přelez") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Route info header
            uiState.route?.let { route ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradeChip(grade = route.grade)
                        Text(route.name, style = MaterialTheme.typography.titleLarge)
                    }
                    if (route.description.isNotBlank()) {
                        Text(
                            text = route.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Date
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
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // Style
            SectionLabel("Styl přelezu")
            StyleSelector(
                selected = uiState.style,
                onSelect = viewModel::onStyleChange
            )

            // Attempts
            SectionLabel("Počet pokusů")
            AttemptsPicker(
                count = uiState.attempts,
                onDecrement = { viewModel.onAttemptsChange(uiState.attempts - 1) },
                onIncrement = { viewModel.onAttemptsChange(uiState.attempts + 1) },
                onManualChange = { v -> v.toIntOrNull()?.let { viewModel.onAttemptsChange(it) } }
            )

            // Personal note
            SectionLabel("Soukromá poznámka")
            OutlinedTextField(
                value = uiState.personalNote,
                onValueChange = viewModel::onPersonalNoteChange,
                placeholder = { Text("Co ti šlo, co ne…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Public note
            SectionLabel("Veřejná poznámka")
            OutlinedTextField(
                value = uiState.publicNote,
                onValueChange = viewModel::onPublicNoteChange,
                placeholder = { Text("Tip pro ostatní lezce…") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            // Personal grade
            SectionLabel("Vlastní hodnocení obtížnosti (volitelné)")
            OutlinedTextField(
                value = uiState.personalGrade ?: "",
                onValueChange = viewModel::onPersonalGradeChange,
                placeholder = { Text("např. 6c+") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Rating
            SectionLabel("Hodnocení cesty")
            StarRating(
                rating = uiState.rating,
                onRatingChange = viewModel::onRatingChange
            )

            // Photos
            SectionLabel("Fotografie přelezu")
            PhotoSection(
                photos = uiState.existingPhotos,
                pendingUris = uiState.pendingPhotoUris,
                onPhotosAdded = viewModel::addPhotos,
                onDeleteSaved = viewModel::deleteExistingPhoto,
                onDeletePending = viewModel::deletePendingPhoto
            )

            HorizontalDivider()

            // Lezec sync toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Zapsat na lezec.cz", style = MaterialTheme.typography.bodyLarge)
                    if (!uiState.lezecHasCredentials) {
                        Text(
                            "Přihlašovací údaje nejsou uloženy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (uiState.route?.lezecId == null) {
                        Text(
                            "Cesta není na lezec.cz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = uiState.syncToLezec,
                    onCheckedChange = viewModel::onSyncToLezecToggle,
                    enabled = uiState.route?.lezecId != null
                )
            }

            Spacer(Modifier.height(8.dp))

            // Save button
            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                colors = if (uiState.lezecSyncState == LezecSyncState.FAILED || uiState.lezecSyncState == LezecSyncState.NO_ID)
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else
                    ButtonDefaults.buttonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                when (uiState.lezecSyncState) {
                    LezecSyncState.SYNCING -> {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Zapisuji na lezec.cz…", style = MaterialTheme.typography.labelLarge)
                    }
                    LezecSyncState.FAILED  -> Text("Chyba synchronizace s lezec.cz", style = MaterialTheme.typography.labelLarge)
                    LezecSyncState.NO_ID   -> Text("Cesta nenalezena na lezec.cz", style = MaterialTheme.typography.labelLarge)
                    else -> {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Text("Uložit přelez", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }

    if (uiState.showCredentialsDialog) {
        LezecCredentialsDialog(
            onConfirm = viewModel::onCredentialsSaved,
            onDismiss = viewModel::onCredentialsDismissed
        )
    }
}

@Composable
private fun LezecCredentialsDialog(
    onConfirm: (uid: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var uid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přihlášení na lezec.cz") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uid,
                    onValueChange = { uid = it },
                    label = { Text("Uživatelské jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Heslo") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(uid, password) },
                enabled = uid.isNotBlank() && password.isNotBlank()
            ) { Text("Uložit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleSelector(selected: AscentStyle, onSelect: (AscentStyle) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AscentStyle.entries.forEach { style ->
            FilterChip(
                selected = style == selected,
                onClick = { onSelect(style) },
                label = { Text(style.label) }
            )
        }
    }
}

@Composable
private fun AttemptsPicker(
    count: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onManualChange: (String) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalIconButton(onClick = onDecrement, enabled = count > 1) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        OutlinedTextField(
            value = count.toString(),
            onValueChange = onManualChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(80.dp)
        )
        FilledTonalIconButton(onClick = onIncrement) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun StarRating(rating: Int?, onRatingChange: (Int?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { star ->
            TextButton(
                onClick = {
                    onRatingChange(if (rating == star) null else star)
                },
                contentPadding = PaddingValues(4.dp)
            ) {
                Text(
                    text = if (rating != null && star <= rating) "★" else "☆",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (rating != null && star <= rating)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

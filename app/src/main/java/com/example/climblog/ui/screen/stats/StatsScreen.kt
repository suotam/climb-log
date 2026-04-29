package com.example.climblog.ui.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.ui.components.SettingsIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(onSettingsClick: () -> Unit = {}, viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiky") },
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

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Filter
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    StatsFilter.entries.forEachIndexed { index, filter ->
                        SegmentedButton(
                            selected = uiState.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            shape = SegmentedButtonDefaults.itemShape(index, StatsFilter.entries.size),
                            label = {
                                Text(when (filter) {
                                    StatsFilter.VSE -> "Vše"
                                    StatsFilter.SKALY -> "Skály"
                                    StatsFilter.STENY -> "Stěny"
                                })
                            }
                        )
                    }
                }
            }

            when (uiState.filter) {
                StatsFilter.SKALY -> {
                    item { OutdoorSummaryRow(uiState) }
                    item { OutdoorStyleCard(uiState) }
                    if (uiState.pyramid.isNotEmpty()) {
                        item { GradePyramidCard(uiState.pyramid) }
                    }
                }
                StatsFilter.STENY -> {
                    item { WallSummaryRow(uiState) }
                    if (uiState.wallStats.wallBreakdown.isNotEmpty()) {
                        item { WallBreakdownCard(uiState) }
                    }
                    if (uiState.wallStats.topGrades.isNotEmpty()) {
                        item { WallGradeCard(uiState) }
                    }
                }
                StatsFilter.VSE -> {
                    item { CombinedSummaryRow(uiState) }
                    item { OutdoorStyleCard(uiState) }
                    if (uiState.pyramid.isNotEmpty()) {
                        item { GradePyramidCard(uiState.pyramid) }
                    }
                    if (uiState.wallStats.wallBreakdown.isNotEmpty()) {
                        item { WallBreakdownCard(uiState) }
                    }
                }
            }
        }
    }
}

// ── Summary rows ───────────────────────────────────────────────────────────────

@Composable
private fun OutdoorSummaryRow(uiState: StatsUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Přelezy celkem", uiState.totalAscents.toString(), Modifier.weight(1f))
        StatCard("V sáčku", uiState.sentRoutes.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun WallSummaryRow(uiState: StatsUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Tréninky", uiState.wallStats.totalSessions.toString(), Modifier.weight(1f))
        StatCard("Cesty celkem", uiState.wallStats.totalRoutes.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun CombinedSummaryRow(uiState: StatsUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("Přelezy\nvenku", uiState.totalAscents.toString(), Modifier.weight(1f))
        StatCard("Tréninky\nv hale", uiState.wallStats.totalSessions.toString(), Modifier.weight(1f))
        StatCard("Cesty\nv hale", uiState.wallStats.totalRoutes.toString(), Modifier.weight(1f))
    }
}

// ── Outdoor style chart ────────────────────────────────────────────────────────

@Composable
private fun OutdoorStyleCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Přelezy podle stylu", style = MaterialTheme.typography.titleMedium)
            if (uiState.ascentsByStyle.isEmpty()) {
                Text("Zatím žádné přelezy. Vylez a zapiš!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxCount = uiState.ascentsByStyle.values.maxOrNull() ?: 1
                AscentStyle.entries.forEach { style ->
                    val count = uiState.ascentsByStyle[style] ?: 0
                    if (count > 0) StyleBar(label = style.label, count = count, maxCount = maxCount)
                }
            }
        }
    }
}

// ── Wall breakdown ─────────────────────────────────────────────────────────────

@Composable
private fun WallBreakdownCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tréninky podle stěny", style = MaterialTheme.typography.titleMedium)
            val maxCount = uiState.wallStats.wallBreakdown.maxOfOrNull { it.second } ?: 1
            uiState.wallStats.wallBreakdown.forEach { (name, count) ->
                StyleBar(label = name, count = count, maxCount = maxCount, labelWidth = 120.dp)
            }
        }
    }
}

// ── Wall grade chart ───────────────────────────────────────────────────────────

@Composable
private fun WallGradeCard(uiState: StatsUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Nejčastější obtížnosti", style = MaterialTheme.typography.titleMedium)
            val maxCount = uiState.wallStats.topGrades.maxOfOrNull { it.second } ?: 1
            uiState.wallStats.topGrades.forEach { (grade, count) ->
                StyleBar(label = grade, count = count, maxCount = maxCount)
            }
        }
    }
}

// ── Shared components ──────────────────────────────────────────────────────────

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── Grade pyramid ──────────────────────────────────────────────────────────────

private val COLOR_ONSIGHT  = Color(0xFF4CAF50)
private val COLOR_FLASH    = Color(0xFFFF9800)
private val COLOR_REDPOINT = Color(0xFF2196F3)
private val COLOR_TOPROPE  = Color(0xFF9E9E9E)

@Composable
private fun GradePyramidCard(pyramid: List<GradePyramidRow>) {
    val maxTotal = pyramid.maxOf { it.total }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Pyramida stupňů", style = MaterialTheme.typography.titleMedium)
            // Legend
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                listOf("OS" to COLOR_ONSIGHT, "Flash" to COLOR_FLASH, "RP" to COLOR_REDPOINT, "TR" to COLOR_TOPROPE)
                    .forEach { (label, color) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
            }
            pyramid.forEach { row -> PyramidBar(row = row, maxTotal = maxTotal) }
        }
    }
}

@Composable
private fun PyramidBar(row: GradePyramidRow, maxTotal: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(row.grade, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(40.dp))
        Box(
            modifier = Modifier.weight(1f).height(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            val fraction = row.total.toFloat() / maxTotal
            Row(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction)) {
                val segments = listOf(
                    row.onsight  to COLOR_ONSIGHT,
                    row.flash    to COLOR_FLASH,
                    row.redpoint to COLOR_REDPOINT,
                    row.toprope  to COLOR_TOPROPE,
                )
                segments.forEach { (count, color) ->
                    if (count > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(count.toFloat())
                                .background(color)
                        )
                    }
                }
            }
        }
        Text(row.total.toString(), style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(24.dp))
    }
}

@Composable
private fun StyleBar(
    label: String,
    count: Int,
    maxCount: Int,
    labelWidth: androidx.compose.ui.unit.Dp = 80.dp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(labelWidth))
        Box(
            modifier = Modifier.weight(1f).height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight()
                    .fillMaxWidth(count.toFloat() / maxCount)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(count.toString(), style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(28.dp))
    }
}

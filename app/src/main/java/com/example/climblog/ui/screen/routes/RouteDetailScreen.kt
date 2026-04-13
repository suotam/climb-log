package com.example.climblog.ui.screen.routes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.RouteComment
import com.example.climblog.ui.components.AscentStyleChip
import com.example.climblog.ui.components.GradeChip
import com.example.climblog.ui.components.PhotoSection
import com.example.climblog.ui.screen.wishlist.PriorityIndicator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateFormat = SimpleDateFormat("d. M. yyyy", Locale("cs"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    routeId: Long,
    onLogAscent: () -> Unit,
    onEditRoute: () -> Unit,
    onNavigateUp: () -> Unit,
    viewModel: RouteDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteAscentDialog by remember { mutableStateOf<Ascent?>(null) }
    var showDeleteRouteDialog by remember { mutableStateOf(false) }
    var showWishlistDialog by remember { mutableStateOf(false) }
    var showAddCommentDialog by remember { mutableStateOf(false) }
    var deleteCommentTarget by remember { mutableStateOf<RouteComment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.route?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = onEditRoute) {
                        Icon(Icons.Filled.Edit, contentDescription = "Upravit cestu")
                    }
                    IconButton(onClick = { showDeleteRouteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete, contentDescription = "Smazat cestu",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val isWishlisted = uiState.wishlistEntry != null
                    IconButton(onClick = {
                        if (isWishlisted) viewModel.removeFromWishlist()
                        else showWishlistDialog = true
                    }) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = if (isWishlisted) "Odebrat z cílů" else "Přidat do cílů",
                            tint = if (isWishlisted) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onLogAscent,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Zaznamenat přelez") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val route = uiState.route ?: return@Scaffold

        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route info
            item { RouteInfoCard(route = route, uiState = uiState) }

            // Wishlist note (if wishlisted)
            uiState.wishlistEntry?.let { entry ->
                item {
                    WishlistNoteCard(
                        entry = entry,
                        onEdit = { showWishlistDialog = true }
                    )
                }
            }

            // Route photos
            item {
                Text("Fotografie cesty", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp))
            }
            item {
                PhotoSection(
                    photos = uiState.routePhotos,
                    onPhotosAdded = viewModel::addRoutePhotos,
                    onDeleteSaved = viewModel::deleteRoutePhoto
                )
            }

            // Comments
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Komentáře (${uiState.comments.size})", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showAddCommentDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Přidat")
                    }
                }
            }
            if (uiState.comments.isEmpty()) {
                item {
                    Text(
                        "Zatím žádné komentáře",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            } else {
                items(uiState.comments, key = { "comment_${it.id}" }) { comment ->
                    CommentCard(comment = comment, onDelete = { deleteCommentTarget = comment })
                }
            }

            // Ascents
            if (uiState.ascents.isNotEmpty()) {
                item {
                    Text("Moje přelezy (${uiState.ascents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp))
                }
                items(uiState.ascents, key = { it.id }) { ascent ->
                    AscentCard(ascent = ascent, onDelete = { showDeleteAscentDialog = ascent })
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Žádný přelez — klikni na tlačítko níže",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showDeleteRouteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteRouteDialog = false },
            title = { Text("Smazat cestu?") },
            text = { Text("\"${uiState.route?.name}\" bude trvale smazána včetně všech přelezů a komentářů.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteRouteDialog = false
                    viewModel.deleteRoute(onDeleted = onNavigateUp)
                }) { Text("Smazat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRouteDialog = false }) { Text("Zrušit") }
            }
        )
    }

    showDeleteAscentDialog?.let { ascent ->
        AlertDialog(
            onDismissRequest = { showDeleteAscentDialog = null },
            title = { Text("Smazat přelez?") },
            text = { Text("Tato akce je nevratná.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAscent(ascent)
                    showDeleteAscentDialog = null
                }) { Text("Smazat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAscentDialog = null }) { Text("Zrušit") }
            }
        )
    }

    if (showWishlistDialog) {
        WishlistEditDialog(
            existing = uiState.wishlistEntry,
            onConfirm = { note, priority ->
                if (uiState.wishlistEntry != null) viewModel.updateWishlist(note, priority)
                else viewModel.addToWishlist(note, priority)
                showWishlistDialog = false
            },
            onDismiss = { showWishlistDialog = false }
        )
    }

    if (showAddCommentDialog) {
        AddCommentDialog(
            onConfirm = { author, text ->
                viewModel.addComment(author, text)
                showAddCommentDialog = false
            },
            onDismiss = { showAddCommentDialog = false }
        )
    }

    deleteCommentTarget?.let { comment ->
        AlertDialog(
            onDismissRequest = { deleteCommentTarget = null },
            title = { Text("Smazat komentář?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteComment(comment)
                    deleteCommentTarget = null
                }) { Text("Smazat", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteCommentTarget = null }) { Text("Zrušit") }
            }
        )
    }
}

// ── Sub-composables ────────────────────────────────────────────────────────────

@Composable
private fun RouteInfoCard(route: Route, uiState: RouteDetailUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradeChip(grade = route.grade)
                uiState.bestStyle?.let { AscentStyleChip(style = it) }
                Text(route.type.label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (route.description.isNotBlank()) {
                Text(route.description, style = MaterialTheme.typography.bodyMedium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                route.length?.let { Text("${it}m", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                route.bolts?.let { Text("$it boltů", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                route.firstAscent?.let {
                    val year = route.firstAscentYear?.let { y -> " ($y)" } ?: ""
                    Text("FA: $it$year", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WishlistNoteCard(
    entry: com.example.climblog.domain.model.WishlistEntry,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PriorityIndicator(priority = entry.priority)
                TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                    Text("Upravit", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (entry.note.isNotBlank()) {
                Text(entry.note, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

@Composable
private fun CommentCard(comment: RouteComment, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(comment.authorName, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                    Text(
                        dateFormat.format(Date(comment.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(comment.text, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Smazat komentář",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AscentCard(ascent: Ascent, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AscentStyleChip(style = ascent.style)
                    Text(dateFormat.format(Date(ascent.date)), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (ascent.attempts > 1) {
                    Text("${ascent.attempts} pokusů", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (ascent.personalNote.isNotBlank()) {
                    Text(ascent.personalNote, style = MaterialTheme.typography.bodyMedium)
                }
                ascent.personalGrade?.let {
                    Text("Můj stupeň: $it", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                ascent.rating?.let { r ->
                    Text("★".repeat(r) + "☆".repeat(5 - r), style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Smazat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Dialogs ────────────────────────────────────────────────────────────────────

@Composable
private fun WishlistEditDialog(
    existing: com.example.climblog.domain.model.WishlistEntry?,
    onConfirm: (note: String, priority: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var note by remember { mutableStateOf(existing?.note ?: "") }
    var priority by remember { mutableIntStateOf(existing?.priority ?: 2) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Upravit cíl" else "Přidat do cílů") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Priority selector
                Text("Priorita", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "Nízká", 2 to "Střední", 3 to "Vysoká").forEach { (p, label) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Poznámka (volitelné)") },
                    placeholder = { Text("Proč chci tuto cestu přelézt…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note, priority) }) { Text("Uložit") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}

@Composable
private fun AddCommentDialog(
    onConfirm: (author: String, text: String) -> Unit,
    onDismiss: () -> Unit
) {
    var author by remember { mutableStateOf("Já") }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat komentář") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Komentář") },
                    placeholder = { Text("Beta, podmínky, tip…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(author.ifBlank { "Já" }, text) },
                enabled = text.isNotBlank()
            ) { Text("Přidat") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}

package com.example.climblog.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.climblog.domain.model.Photo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable photo gallery section.
 *
 * Shows existing [photos] as a horizontal row of thumbnails + an "Add" button.
 * Handles gallery picking and camera capture internally.
 *
 * @param photos already-persisted photos to display
 * @param pendingUris photo URIs staged for saving (not yet in DB, e.g. during ascent form)
 * @param onPhotosAdded called with new URI strings to add
 * @param onDeleteSaved called when user removes an already-persisted photo
 * @param onDeletePending called when user removes a pending (not yet saved) URI
 */
@Composable
fun PhotoSection(
    photos: List<Photo>,
    pendingUris: List<String> = emptyList(),
    onPhotosAdded: (List<String>) -> Unit,
    onDeleteSaved: (Photo) -> Unit = {},
    onDeletePending: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPickerDialog by remember { mutableStateOf(false) }
    var fullscreenUri by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            coroutineScope.launch {
                val localUris = copyPhotosToLocalStorage(context, uris)
                if (localUris.isNotEmpty()) onPhotosAdded(localUris)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingCameraFile?.let { onPhotosAdded(listOf(Uri.fromFile(it).toString())) }
        }
        pendingCameraUri = null
        pendingCameraFile = null
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val file = createTempPhotoFile(context)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCameraFile = file
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            // Add button
            item {
                AddPhotoButton(onClick = { showPickerDialog = true })
            }

            // Persisted photos
            items(photos, key = { "saved_${it.id}" }) { photo ->
                PhotoThumbnail(
                    uri = photo.uri,
                    onDelete = { onDeleteSaved(photo) },
                    onClick = { fullscreenUri = photo.uri }
                )
            }

            // Pending (not yet saved) photos
            items(pendingUris, key = { "pending_$it" }) { uri ->
                PhotoThumbnail(
                    uri = uri,
                    onDelete = { onDeletePending(uri) },
                    onClick = { fullscreenUri = uri }
                )
            }
        }
    }

    if (showPickerDialog) {
        PickerDialog(
            onCamera = {
                showPickerDialog = false
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    val file = createTempPhotoFile(context)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    pendingCameraFile = file
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGallery = {
                showPickerDialog = false
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onDismiss = { showPickerDialog = false }
        )
    }

    fullscreenUri?.let { uri ->
        FullscreenPhotoDialog(uri = uri, onDismiss = { fullscreenUri = null })
    }
}

@Composable
private fun AddPhotoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.AddAPhoto,
                contentDescription = "Přidat fotku",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Foto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PhotoThumbnail(uri: String, onDelete: () -> Unit, onClick: () -> Unit) {
    Box(modifier = Modifier.size(80.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(Uri.parse(uri))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
        )
        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(22.dp)
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Odstranit fotku",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PickerDialog(
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Přidat fotku") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCamera)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Fotoaparát")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onGallery)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Galerie")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Zrušit") }
        }
    )
}

@Composable
private fun FullscreenPhotoDialog(uri: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onDismiss)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(uri))
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun createTempPhotoFile(context: android.content.Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File.createTempFile("PHOTO_${timestamp}_", ".jpg", dir)
}

private suspend fun copyPhotosToLocalStorage(context: Context, uris: List<Uri>): List<String> =
    withContext(Dispatchers.IO) {
        uris.mapNotNull { sourceUri ->
            runCatching {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    val target = createTempPhotoFile(context)
                    target.outputStream().use { output -> input.copyTo(output) }
                    Uri.fromFile(target).toString()
                }
            }.getOrNull()
        }
    }

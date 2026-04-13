package com.example.climblog.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.climblog.domain.model.Wall
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Map showing [walls] with GPS coordinates as markers.
 * Clicking a marker calls [onWallClick] with the wall.
 */
@Composable
fun WallMapView(
    walls: List<Wall>,
    selectedWallId: Long?,
    onWallClick: (Wall) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    remember(Unit) {
        Configuration.getInstance().apply {
            load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
            userAgentValue = context.packageName
            osmdroidTileCache = File(context.cacheDir, "osmdroid_tiles")
        }
    }

    val wallsWithCoords = remember(walls) {
        walls.filter { it.latitude != null && it.longitude != null }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            controller.setZoom(11.0)
            controller.setCenter(GeoPoint(50.075, 14.42)) // Praha
        }
    }

    LaunchedEffect(wallsWithCoords, selectedWallId) {
        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Marker>().toSet())

        wallsWithCoords.forEach { wall ->
            val point = GeoPoint(wall.latitude!!, wall.longitude!!)
            Marker(mapView).apply {
                position = point
                title = wall.name
                snippet = wall.city
                infoWindow = null
                alpha = if (selectedWallId == null || selectedWallId == wall.id) 1f else 0.5f
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onWallClick(wall)
                    true
                }
                mapView.overlays.add(this)
            }
        }

        if (selectedWallId != null) {
            wallsWithCoords.find { it.id == selectedWallId }?.let {
                mapView.controller.animateTo(GeoPoint(it.latitude!!, it.longitude!!), 13.0, 600L)
            }
        } else if (wallsWithCoords.size >= 2) {
            val points = wallsWithCoords.map { GeoPoint(it.latitude!!, it.longitude!!) }
            val box = BoundingBox.fromGeoPoints(points)
            mapView.post { mapView.zoomToBoundingBox(box.increaseByScale(1.4f), true, 120) }
        }

        mapView.invalidate()
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

package com.example.climblog.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
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
import com.example.climblog.domain.model.Area
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

/**
 * Reusable OSMDroid map composable.
 *
 * Shows [areas] as markers. Clicking a marker calls [onMarkerClick] with the area ID.
 */
@Composable
fun OsmMapView(
    areas: List<Area>,
    onMarkerClick: (Long) -> Unit,
    selectedAreaId: Long? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Configure OSMDroid (idempotent, safe to call multiple times)
    remember(Unit) { initOsmdroid(context) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            minZoomLevel = 5.0
            maxZoomLevel = 19.0
            // Default: Czech Republic
            controller.setZoom(7.5)
            controller.setCenter(GeoPoint(49.8, 15.5))
        }
    }

    val areasWithCoords = remember(areas) {
        areas.filter { it.latitude != null && it.longitude != null }
    }

    // Rebuild markers when areas or selection changes
    LaunchedEffect(areasWithCoords, selectedAreaId) {
        val markersToRemove = mapView.overlays.filterIsInstance<Marker>().toList()
        mapView.overlays.removeAll(markersToRemove.toSet())

        areasWithCoords.forEach { area ->
            val point = GeoPoint(area.latitude!!, area.longitude!!)
            Marker(mapView).apply {
                position = point
                title = area.name
                snippet = "${area.region} · ${area.rockType}"
                infoWindow = null  // custom selection card handled in Compose
                alpha = if (selectedAreaId == null || selectedAreaId == area.id) 1f else 0.5f
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onMarkerClick(area.id)
                    true
                }
                mapView.overlays.add(this)
            }
        }

        // Fit all markers in view on first load (only when no selection)
        if (selectedAreaId == null && areasWithCoords.size >= 2) {
            val points = areasWithCoords.map { GeoPoint(it.latitude!!, it.longitude!!) }
            val box = BoundingBox.fromGeoPoints(points)
            mapView.post { mapView.zoomToBoundingBox(box.increaseByScale(1.6f), true, 120) }
        } else if (selectedAreaId != null) {
            val area = areasWithCoords.find { it.id == selectedAreaId }
            area?.let {
                mapView.controller.animateTo(GeoPoint(it.latitude!!, it.longitude!!), 13.0, 600L)
            }
        }

        mapView.invalidate()
    }

    // Lifecycle: pause/resume tile loading with Activity
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

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

private fun initOsmdroid(context: Context) {
    Configuration.getInstance().apply {
        load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        userAgentValue = context.packageName
        // Store tile cache in app-specific directory (no storage permission needed)
        osmdroidTileCache = File(context.cacheDir, "osmdroid_tiles")
    }
}

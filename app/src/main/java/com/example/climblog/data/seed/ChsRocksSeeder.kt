package com.example.climblog.data.seed

import android.content.Context
import android.util.JsonReader
import android.util.JsonToken
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.entity.AreaEntity
import com.example.climblog.data.local.entity.RouteEntity
import com.example.climblog.data.local.entity.SectorEntity
import com.example.climblog.domain.model.GradeSystem
import com.example.climblog.domain.model.RouteType
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeduje ČHS databázi skal z assets/chs-rocks.json.
 *
 * JSON schéma (chs-complete):
 *   regions[] → groups[] → areas[] (GPS) → sectors[] → routes[]
 *
 * Mapování do DB:
 *   Area.region  = "{regionName} / {groupName}"
 *   AreaEntity   = JSON area  (1 125 položek, má GPS)
 *   SectorEntity = JSON sector (17 971 skál)
 *   RouteEntity  = JSON route
 */
@Singleton
class ChsRocksSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val areaDao: AreaDao,
    private val sectorDao: SectorDao,
    private val routeDao: RouteDao
) {
    companion object {
        private const val PREFS = "chs_seed"
        private const val KEY_FULL_SEED = "seeded_v4"  // plný seed — neměnit, jinak smaže data!
        private const val KEY_LEZEC_IDS  = "lezecids_v1" // přidá lezecId bez mazání dat
    }

    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        // Plný seed — spustí se jen při první instalaci
        if (!prefs.getBoolean(KEY_FULL_SEED, false)) {
            routeDao.deleteChsRoutes()
            sectorDao.deleteChsSectors()
            areaDao.deleteChsAreas()
            context.assets.open("chs-rocks.json").use { stream ->
                val reader = JsonReader(InputStreamReader(stream, Charsets.UTF_8))
                reader.isLenient = true
                parseRoot(reader)
                reader.close()
            }
            prefs.edit().putBoolean(KEY_FULL_SEED, true).apply()
        }

        // Aktualizace lezecId — nemaže žádná data, jen updatuje existující cesty
        if (!prefs.getBoolean(KEY_LEZEC_IDS, false)) {
            context.assets.open("chs-rocks.json").use { stream ->
                val reader = JsonReader(InputStreamReader(stream, Charsets.UTF_8))
                reader.isLenient = true
                updateLezecIds(reader)
                reader.close()
            }
            prefs.edit().putBoolean(KEY_LEZEC_IDS, true).apply()
        }
    }

    private suspend fun updateLezecIds(reader: JsonReader) {
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "regions") updateLezecIdsInRegions(reader)
            else reader.skipValue()
        }
        reader.endObject()
    }

    private suspend fun updateLezecIdsInRegions(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "groups") updateLezecIdsInGroups(reader)
                else reader.skipValue()
            }
            reader.endObject()
        }
        reader.endArray()
    }

    private suspend fun updateLezecIdsInGroups(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "areas") updateLezecIdsInAreas(reader)
                else reader.skipValue()
            }
            reader.endObject()
        }
        reader.endArray()
    }

    private suspend fun updateLezecIdsInAreas(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "sectors") updateLezecIdsInSectors(reader)
                else reader.skipValue()
            }
            reader.endObject()
        }
        reader.endArray()
    }

    private suspend fun updateLezecIdsInSectors(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) {
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "routes") updateLezecIdsInRoutes(reader)
                else reader.skipValue()
            }
            reader.endObject()
        }
        reader.endArray()
    }

    private suspend fun updateLezecIdsInRoutes(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) {
            var routeId = -1L
            var lezecId: Int? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id"      -> routeId = nextLongOrNull(reader) ?: -1L
                    "lezecId" -> lezecId = nextIntOrNull(reader)
                    else      -> reader.skipValue()
                }
            }
            reader.endObject()
            if (routeId != -1L) {
                routeDao.updateLezecId("chs-route-$routeId", lezecId)
            }
        }
        reader.endArray()
    }

    // ── Kořen ────────────────────────────────────────────────────────────────

    private suspend fun parseRoot(reader: JsonReader) {
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "regions") parseRegions(reader) else reader.skipValue()
        }
        reader.endObject()
    }

    // ── Regions ──────────────────────────────────────────────────────────────

    private suspend fun parseRegions(reader: JsonReader) {
        reader.beginArray()
        while (reader.hasNext()) parseRegion(reader)
        reader.endArray()
    }

    private suspend fun parseRegion(reader: JsonReader) {
        var regionName = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name"   -> regionName = nextStringOrNull(reader) ?: ""
                "groups" -> parseGroups(reader, regionName)
                else     -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    // ── Groups (oblasti) ─────────────────────────────────────────────────────

    private suspend fun parseGroups(reader: JsonReader, regionName: String) {
        reader.beginArray()
        while (reader.hasNext()) parseGroup(reader, regionName)
        reader.endArray()
    }

    private suspend fun parseGroup(reader: JsonReader, regionName: String) {
        var groupName = ""
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name"  -> groupName = nextStringOrNull(reader) ?: ""
                "areas" -> parseAreas(reader, "$regionName / $groupName")
                else    -> reader.skipValue()
            }
        }
        reader.endObject()
    }

    // ── Areas (sektory s GPS) ────────────────────────────────────────────────

    private suspend fun parseAreas(reader: JsonReader, regionLabel: String) {
        reader.beginArray()
        while (reader.hasNext()) parseArea(reader, regionLabel)
        reader.endArray()
    }

    private suspend fun parseArea(reader: JsonReader, regionLabel: String) {
        var areaId = -1L
        var areaName = ""
        var lat: Double? = null
        var lng: Double? = null
        var areaRockType = ""
        var areaDescription = ""
        var sectorsArray: Boolean = false

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id"          -> areaId = nextLongOrNull(reader) ?: -1L
                "name"        -> areaName = nextStringOrNull(reader) ?: ""
                "rockType"    -> areaRockType = nextStringOrNull(reader) ?: ""
                "description" -> areaDescription = nextStringOrNull(reader) ?: ""
                "gps"         -> {
                    val coords = parseGps(reader)
                    lat = coords?.first
                    lng = coords?.second
                }
                "sectors"     -> {
                    val insertedId = areaDao.insert(
                        AreaEntity(
                            remoteId = "chs-area-$areaId",
                            name = areaName,
                            country = "CZ",
                            region = regionLabel,
                            latitude = lat,
                            longitude = lng,
                            rockType = areaRockType,
                            description = areaDescription
                        )
                    )
                    parseSectors(reader, insertedId)
                    sectorsArray = true
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // fallback: sectors klíč přišel dřív než name/gps (nemělo by se stát)
        if (!sectorsArray && areaName.isNotEmpty()) {
            areaDao.insert(
                AreaEntity(
                    remoteId = "chs-area-$areaId",
                    name = areaName,
                    country = "CZ",
                    region = regionLabel,
                    latitude = lat,
                    longitude = lng,
                    rockType = areaRockType,
                    description = areaDescription
                )
            )
        }
    }

    /** Parsuje {"lat": ..., "lng": ...} nebo null. */
    private fun parseGps(reader: JsonReader): Pair<Double, Double>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }
        var lat: Double? = null
        var lng: Double? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "lat" -> lat = nextDoubleOrNull(reader)
                "lng" -> lng = nextDoubleOrNull(reader)
                else  -> reader.skipValue()
            }
        }
        reader.endObject()
        return if (lat != null && lng != null) lat to lng else null
    }

    // ── Sectors (skály) ──────────────────────────────────────────────────────

    private suspend fun parseSectors(reader: JsonReader, areaId: Long) {
        reader.beginArray()
        while (reader.hasNext()) parseSector(reader, areaId)
        reader.endArray()
    }

    private suspend fun parseSector(reader: JsonReader, areaId: Long) {
        var sectorId = -1L
        var sectorOrder = 0
        var sectorName = ""
        var sectorDescription = ""
        var routesParsed = false

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id"          -> sectorId = nextLongOrNull(reader) ?: -1L
                "order"       -> sectorOrder = nextIntOrNull(reader) ?: 0
                "name"        -> sectorName = nextStringOrNull(reader) ?: ""
                "description" -> sectorDescription = nextStringOrNull(reader) ?: ""
                "routes"      -> {
                    val insertedId = sectorDao.insert(
                        SectorEntity(
                            remoteId = "chs-sector-$sectorId",
                            areaId = areaId,
                            order = sectorOrder,
                            name = sectorName,
                            description = sectorDescription
                        )
                    )
                    val routes = parseRoutes(reader, insertedId)
                    if (routes.isNotEmpty()) routeDao.insertAll(routes)
                    routesParsed = true
                }
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (!routesParsed && sectorName.isNotEmpty()) {
            sectorDao.insert(
                SectorEntity(
                    remoteId = "chs-sector-$sectorId",
                    areaId = areaId,
                    order = sectorOrder,
                    name = sectorName,
                    description = sectorDescription
                )
            )
        }
    }

    // ── Routes ───────────────────────────────────────────────────────────────

    private fun parseRoutes(reader: JsonReader, sectorId: Long): List<RouteEntity> {
        val routes = mutableListOf<RouteEntity>()
        reader.beginArray()
        while (reader.hasNext()) {
            parseRoute(reader, sectorId)?.let { routes.add(it) }
        }
        reader.endArray()
        return routes
    }

    private fun parseRoute(reader: JsonReader, sectorId: Long): RouteEntity? {
        var id = -1L
        var routeOrder = 0
        var name = ""
        var grade = ""
        var routeType: String? = null
        var length: Int? = null
        var description = ""
        var firstAscent: String? = null
        var lezecId: Int? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id"          -> id = nextLongOrNull(reader) ?: -1L
                "order"       -> routeOrder = nextIntOrNull(reader) ?: 0
                "name"        -> name = nextStringOrNull(reader) ?: ""
                "grade"       -> grade = nextStringOrNull(reader) ?: ""
                "type"        -> routeType = nextStringOrNull(reader)
                "length"      -> length = nextIntOrNull(reader)
                "description" -> description = nextStringOrNull(reader) ?: ""
                "firstAscent" -> firstAscent = nextStringOrNull(reader)
                "lezecId"     -> lezecId = nextIntOrNull(reader)
                else          -> reader.skipValue()
            }
        }
        reader.endObject()

        if (name.isEmpty()) return null

        val resolvedType = when (routeType?.lowercase()) {
            "sport"   -> RouteType.SPORT
            "boulder" -> RouteType.BOULDER
            "trad"    -> RouteType.TRAD
            else      -> RouteType.TRAD
        }

        return RouteEntity(
            remoteId = "chs-route-$id",
            sectorId = sectorId,
            order = routeOrder,
            name = name,
            grade = grade.ifEmpty { "?" },
            gradeSystem = GradeSystem.UIAA.name,
            type = resolvedType.name,
            length = length,
            description = description,
            firstAscent = firstAscent,
            lezecId = lezecId
        )
    }

    // ── Nullable helpers ─────────────────────────────────────────────────────

    private fun nextStringOrNull(reader: JsonReader): String? =
        if (reader.peek() == JsonToken.NULL) { reader.nextNull(); null } else reader.nextString()

    private fun nextLongOrNull(reader: JsonReader): Long? =
        if (reader.peek() == JsonToken.NULL) { reader.nextNull(); null } else reader.nextLong()

    private fun nextIntOrNull(reader: JsonReader): Int? =
        if (reader.peek() == JsonToken.NULL) { reader.nextNull(); null }
        else when (reader.peek()) {
            JsonToken.NUMBER -> reader.nextInt()
            else -> { reader.skipValue(); null }
        }

    private fun nextDoubleOrNull(reader: JsonReader): Double? =
        if (reader.peek() == JsonToken.NULL) { reader.nextNull(); null } else reader.nextDouble()
}

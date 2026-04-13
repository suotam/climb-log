package com.example.climblog.data.seed

import com.example.climblog.data.local.dao.WallDao
import com.example.climblog.data.local.entity.WallEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds preset Prague indoor climbing walls. Runs only if no presets exist yet.
 */
@Singleton
class WallSeeder @Inject constructor(
    private val wallDao: WallDao
) {
    suspend fun seedIfEmpty() {
        if (wallDao.countPresets() > 0) return
        wallDao.insertWalls(PRAGUE_WALLS)
    }
}

private val PRAGUE_WALLS = listOf(
    WallEntity(
        name = "BigWall",
        city = "Praha 9 – Vysočany",
        description = "Největší lezecká stěna v Praze. 20m stěna, 3 000 m² plochy, 115 lanových cest + boulder.",
        latitude = 50.1054,
        longitude = 14.4999,
        isPreset = true
    ),
    WallEntity(
        name = "SmíchOFF",
        city = "Praha 5 – Smíchov",
        description = "Lezecké centrum s lanem i boulderingem v centru Prahy. Křížová 6.",
        latitude = 50.0549,
        longitude = 14.4059,
        isPreset = true
    ),
    WallEntity(
        name = "HUDY Boulder Karlín",
        city = "Praha 8 – Karlín",
        description = "Moderní bouldrovka otevřená 2023. 655 m², 160+ boulderů. Skvělá dostupnost MHD.",
        latitude = 50.0935,
        longitude = 14.4533,
        isPreset = true
    ),
    WallEntity(
        name = "Stěna Ruzyně",
        city = "Praha 6 – Ruzyně",
        description = "Velké indoor + outdoor lezecké centrum. Lano, boulder, kurzy pro děti i dospělé. Ztracená 1.",
        latitude = 50.0820,
        longitude = 14.3079,
        isPreset = true
    ),
    WallEntity(
        name = "Třináctka",
        city = "Praha 13 – Stodůlky",
        description = "Lezecká stěna a bouldrovka u metra Stodůlky. Jeremiášova 2581/2.",
        latitude = 50.0405,
        longitude = 14.3279,
        isPreset = true
    ),
    WallEntity(
        name = "JamJam",
        city = "Praha 6 – Ruzyně",
        description = "Bouldrovka s kavárnou. Vlastina 889, Praha 6.",
        latitude = 50.0931,
        longitude = 14.3155,
        isPreset = true
    ),
    WallEntity(
        name = "Jungle Letňany",
        city = "Praha 9 – Letňany",
        description = "Velké sportovní centrum: lano, boulder, parkour, dětský svět. 125 lanových cest, 2 000 m². Veselská 699.",
        latitude = 50.1417,
        longitude = 14.5176,
        isPreset = true
    )
)

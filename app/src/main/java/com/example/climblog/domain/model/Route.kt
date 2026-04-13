package com.example.climblog.domain.model

data class Route(
    val id: Long = 0,
    val sectorId: Long,
    val name: String,
    val grade: String,
    val gradeSystem: GradeSystem = GradeSystem.FRENCH,
    val type: RouteType = RouteType.SPORT,
    val length: Int? = null,
    val bolts: Int? = null,
    val description: String = "",
    val firstAscent: String? = null,
    val firstAscentYear: Int? = null
)

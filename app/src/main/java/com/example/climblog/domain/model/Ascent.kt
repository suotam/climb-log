package com.example.climblog.domain.model

data class Ascent(
    val id: Long = 0,
    val routeId: Long,
    val userId: String = "local_user",
    val date: Long,           // epoch millis
    val style: AscentStyle,
    val attempts: Int = 1,
    val personalNote: String = "",
    val publicNote: String = "",
    val photoUri: String? = null,
    val personalGrade: String? = null,
    val rating: Int? = null   // 1–5
)

/** Sdružuje cestu s jejím nejlepším přelezem (pro přehledy). */
data class RouteWithBestAscent(
    val route: Route,
    val bestAscent: Ascent?,
    val totalAscents: Int
)

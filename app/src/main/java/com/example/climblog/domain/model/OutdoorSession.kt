package com.example.climblog.domain.model

data class OutdoorSession(
    val id: Long = 0,
    val date: Long,
    val areaId: Long? = null,
    val areaName: String? = null,
    val customCragName: String? = null,
    val notes: String = "",
    val routes: List<OutdoorSessionRoute> = emptyList()
) {
    val cragDisplayName: String get() = areaName ?: customCragName ?: "Neznámá skála"
    val isKnownArea: Boolean get() = areaId != null
}

data class OutdoorSessionRoute(
    val id: Long = 0,
    val sessionId: Long,
    val routeId: Long,
    val routeName: String,
    val grade: String,
    val style: AscentStyle
)

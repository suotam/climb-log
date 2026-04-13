package com.example.climblog.domain.model

data class Photo(
    val id: Long = 0,
    val areaId: Long? = null,
    val routeId: Long? = null,
    val ascentId: Long? = null,
    val outdoorSessionId: Long? = null,
    val uri: String,
    val caption: String? = null,
    val takenAt: Long = System.currentTimeMillis()
)

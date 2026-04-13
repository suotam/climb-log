package com.example.climblog.domain.model

data class RouteComment(
    val id: Long = 0,
    val routeId: Long,
    val authorName: String = "Já",
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

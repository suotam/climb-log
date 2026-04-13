package com.example.climblog.domain.model

data class Sector(
    val id: Long = 0,
    val areaId: Long,
    val name: String,
    val description: String = "",
    val approach: String = ""
)

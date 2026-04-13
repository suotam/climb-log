package com.example.climblog.domain.model

data class Area(
    val id: Long = 0,
    val name: String,
    val country: String = "CZ",
    val region: String = "",
    val description: String = "",
    val rockType: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

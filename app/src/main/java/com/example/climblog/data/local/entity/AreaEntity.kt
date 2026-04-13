package com.example.climblog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.climblog.domain.model.Area

@Entity(tableName = "areas")
data class AreaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val name: String,
    val country: String = "CZ",
    val region: String = "",
    val description: String = "",
    val rockType: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val syncStatus: String = "LOCAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun AreaEntity.toDomain() = Area(
    id = id,
    name = name,
    country = country,
    region = region,
    description = description,
    rockType = rockType,
    latitude = latitude,
    longitude = longitude
)

fun Area.toEntity() = AreaEntity(
    id = id,
    name = name,
    country = country,
    region = region,
    description = description,
    rockType = rockType,
    latitude = latitude,
    longitude = longitude
)

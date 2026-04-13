package com.example.climblog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.climblog.domain.model.Photo

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = AreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["areaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AscentEntity::class,
            parentColumns = ["id"],
            childColumns = ["ascentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OutdoorSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["outdoorSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("areaId"), Index("routeId"), Index("ascentId"), Index("outdoorSessionId")]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val areaId: Long? = null,
    val routeId: Long? = null,
    val ascentId: Long? = null,
    val outdoorSessionId: Long? = null,
    val uri: String,
    val caption: String? = null,
    val takenAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "LOCAL",
    val createdAt: Long = System.currentTimeMillis()
)

fun PhotoEntity.toDomain() = Photo(
    id = id,
    areaId = areaId,
    routeId = routeId,
    ascentId = ascentId,
    outdoorSessionId = outdoorSessionId,
    uri = uri,
    caption = caption,
    takenAt = takenAt
)

fun Photo.toEntity() = PhotoEntity(
    id = id,
    areaId = areaId,
    routeId = routeId,
    ascentId = ascentId,
    outdoorSessionId = outdoorSessionId,
    uri = uri,
    caption = caption,
    takenAt = takenAt
)

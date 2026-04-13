package com.example.climblog.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.OutdoorSession

@Entity(tableName = "outdoor_sessions")
data class OutdoorSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val areaId: Long? = null,
    val customCragName: String? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "outdoor_session_routes",
    foreignKeys = [
        ForeignKey(
            entity = OutdoorSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("routeId")]
)
data class OutdoorSessionRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val routeId: Long,
    val style: String = AscentStyle.REDPOINT.name
)

data class OutdoorSessionWithRoutes(
    @Embedded val session: OutdoorSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val routes: List<OutdoorSessionRouteEntity>
)

fun OutdoorSessionWithRoutes.toDomain(
    areaName: String?,
    routeDetails: List<com.example.climblog.domain.model.OutdoorSessionRoute>
) = OutdoorSession(
    id = session.id,
    date = session.date,
    areaId = session.areaId,
    areaName = areaName,
    customCragName = session.customCragName,
    notes = session.notes,
    routes = routeDetails
)

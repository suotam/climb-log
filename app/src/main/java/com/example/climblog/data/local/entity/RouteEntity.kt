package com.example.climblog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.climblog.domain.model.GradeSystem
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.RouteType

@Entity(
    tableName = "routes",
    foreignKeys = [ForeignKey(
        entity = SectorEntity::class,
        parentColumns = ["id"],
        childColumns = ["sectorId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sectorId")]
)
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val sectorId: Long,
    val order: Int = 0,
    val name: String,
    val grade: String,
    val gradeSystem: String = GradeSystem.FRENCH.name,
    val type: String = RouteType.SPORT.name,
    val length: Int? = null,
    val bolts: Int? = null,
    val description: String = "",
    val firstAscent: String? = null,
    val firstAscentYear: Int? = null,
    val syncStatus: String = "LOCAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lezecId: Int? = null
)

fun RouteEntity.toDomain() = Route(
    id = id,
    sectorId = sectorId,
    name = name,
    grade = grade,
    gradeSystem = GradeSystem.valueOf(gradeSystem),
    type = RouteType.valueOf(type),
    length = length,
    bolts = bolts,
    description = description,
    firstAscent = firstAscent,
    firstAscentYear = firstAscentYear,
    lezecId = lezecId
)

fun Route.toEntity() = RouteEntity(
    id = id,
    sectorId = sectorId,
    name = name,
    grade = grade,
    gradeSystem = gradeSystem.name,
    type = type.name,
    length = length,
    bolts = bolts,
    description = description,
    firstAscent = firstAscent,
    firstAscentYear = firstAscentYear
)

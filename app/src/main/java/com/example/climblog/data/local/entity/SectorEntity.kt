package com.example.climblog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.climblog.domain.model.Sector

@Entity(
    tableName = "sectors",
    foreignKeys = [ForeignKey(
        entity = AreaEntity::class,
        parentColumns = ["id"],
        childColumns = ["areaId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("areaId")]
)
data class SectorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String? = null,
    val areaId: Long,
    val order: Int = 0,
    val name: String,
    val description: String = "",
    val approach: String = "",
    val syncStatus: String = "LOCAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun SectorEntity.toDomain() = Sector(
    id = id,
    areaId = areaId,
    name = name,
    description = description,
    approach = approach
)

fun Sector.toEntity() = SectorEntity(
    id = id,
    areaId = areaId,
    name = name,
    description = description,
    approach = approach
)

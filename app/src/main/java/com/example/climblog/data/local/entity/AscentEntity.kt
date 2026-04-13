package com.example.climblog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.climblog.domain.model.Ascent
import com.example.climblog.domain.model.AscentStyle

@Entity(
    tableName = "ascents",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class AscentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val userId: String = "local_user",
    val date: Long,
    val style: String,
    val attempts: Int = 1,
    val personalNote: String = "",
    val publicNote: String = "",
    val photoUri: String? = null,
    val personalGrade: String? = null,
    val rating: Int? = null,
    val syncStatus: String = "LOCAL",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

fun AscentEntity.toDomain() = Ascent(
    id = id,
    routeId = routeId,
    userId = userId,
    date = date,
    style = AscentStyle.valueOf(style),
    attempts = attempts,
    personalNote = personalNote,
    publicNote = publicNote,
    photoUri = photoUri,
    personalGrade = personalGrade,
    rating = rating
)

fun Ascent.toEntity() = AscentEntity(
    id = id,
    routeId = routeId,
    userId = userId,
    date = date,
    style = style.name,
    attempts = attempts,
    personalNote = personalNote,
    publicNote = publicNote,
    photoUri = photoUri,
    personalGrade = personalGrade,
    rating = rating
)

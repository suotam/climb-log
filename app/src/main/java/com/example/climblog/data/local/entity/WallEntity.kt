package com.example.climblog.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.climblog.domain.model.SessionType
import com.example.climblog.domain.model.Wall
import com.example.climblog.domain.model.WallGradeEntry
import com.example.climblog.domain.model.WallSession

// ── Wall ──────────────────────────────────────────────────────────────────────

@Entity(tableName = "walls")
data class WallEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val city: String = "",
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isPreset: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun WallEntity.toDomain() = Wall(id = id, name = name, city = city, description = description, latitude = latitude, longitude = longitude, isPreset = isPreset)
fun Wall.toEntity() = WallEntity(id = id, name = name, city = city, description = description, latitude = latitude, longitude = longitude, isPreset = isPreset)

// ── WallSession ───────────────────────────────────────────────────────────────

@Entity(
    tableName = "wall_sessions",
    foreignKeys = [ForeignKey(
        entity = WallEntity::class,
        parentColumns = ["id"],
        childColumns = ["wallId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("wallId")]
)
data class WallSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wallId: Long,
    val date: Long,
    val type: String, // SessionType.name
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ── WallGradeEntry ────────────────────────────────────────────────────────────

@Entity(
    tableName = "wall_grade_entries",
    foreignKeys = [ForeignKey(
        entity = WallSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class WallGradeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val grade: String,
    val count: Int
)

fun WallGradeEntryEntity.toDomain() = WallGradeEntry(id = id, sessionId = sessionId, grade = grade, count = count)

// ── Join result ───────────────────────────────────────────────────────────────

data class WallSessionWithGrades(
    @Embedded val session: WallSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val grades: List<WallGradeEntryEntity>,
    @Relation(parentColumn = "wallId", entityColumn = "id")
    val wall: WallEntity?
)

fun WallSessionWithGrades.toDomain() = WallSession(
    id = session.id,
    wallId = session.wallId,
    wallName = wall?.name ?: "",
    date = session.date,
    type = SessionType.valueOf(session.type),
    notes = session.notes,
    grades = grades.map { it.toDomain() }
)

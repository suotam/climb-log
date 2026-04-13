package com.example.climblog.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.climblog.domain.model.RouteComment

@Entity(
    tableName = "route_comments",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class RouteCommentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val authorName: String = "Já",
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

fun RouteCommentEntity.toDomain() = RouteComment(
    id = id,
    routeId = routeId,
    authorName = authorName,
    text = text,
    createdAt = createdAt
)

fun RouteComment.toEntity() = RouteCommentEntity(
    id = id,
    routeId = routeId,
    authorName = authorName,
    text = text,
    createdAt = createdAt
)

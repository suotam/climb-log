package com.example.climblog.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.example.climblog.domain.model.Route
import com.example.climblog.domain.model.WishlistEntry

@Entity(
    tableName = "wishlists",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId", unique = true)]
)
data class WishlistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routeId: Long,
    val note: String = "",
    val priority: Int = 2, // 1 = nízká, 2 = střední, 3 = vysoká
    val addedAt: Long = System.currentTimeMillis()
)

data class WishlistWithRoute(
    @Embedded val wishlist: WishlistEntity,
    @Relation(parentColumn = "routeId", entityColumn = "id")
    val route: RouteEntity?
)

fun WishlistWithRoute.toDomain() = WishlistEntry(
    id = wishlist.id,
    routeId = wishlist.routeId,
    route = route?.toDomain(),
    note = wishlist.note,
    priority = wishlist.priority,
    addedAt = wishlist.addedAt
)

fun WishlistEntity.toDomain(route: Route? = null) = WishlistEntry(
    id = id,
    routeId = routeId,
    route = route,
    note = note,
    priority = priority,
    addedAt = addedAt
)

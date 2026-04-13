package com.example.climblog.data.repository

import com.example.climblog.domain.model.WishlistEntry
import kotlinx.coroutines.flow.Flow

interface WishlistRepository {
    fun getWishlist(): Flow<List<WishlistEntry>>
    fun getWishlistEntryForRoute(routeId: Long): Flow<WishlistEntry?>
    suspend fun addToWishlist(routeId: Long, note: String, priority: Int): Long
    suspend fun updateWishlistEntry(entry: WishlistEntry)
    suspend fun removeFromWishlist(routeId: Long)
}

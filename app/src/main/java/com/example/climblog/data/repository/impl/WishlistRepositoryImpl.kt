package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.WishlistDao
import com.example.climblog.data.local.entity.WishlistEntity
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.repository.WishlistRepository
import com.example.climblog.domain.model.WishlistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WishlistRepositoryImpl @Inject constructor(
    private val wishlistDao: WishlistDao
) : WishlistRepository {

    override fun getWishlist(): Flow<List<WishlistEntry>> =
        wishlistDao.getWishlistWithRoutes().map { list -> list.map { it.toDomain() } }

    override fun getWishlistEntryForRoute(routeId: Long): Flow<WishlistEntry?> =
        wishlistDao.getByRoute(routeId).map { it?.toDomain() }

    override suspend fun addToWishlist(routeId: Long, note: String, priority: Int): Long =
        wishlistDao.insert(WishlistEntity(routeId = routeId, note = note, priority = priority))

    override suspend fun updateWishlistEntry(entry: WishlistEntry) =
        wishlistDao.update(WishlistEntity(
            id = entry.id,
            routeId = entry.routeId,
            note = entry.note,
            priority = entry.priority,
            addedAt = entry.addedAt
        ))

    override suspend fun removeFromWishlist(routeId: Long) =
        wishlistDao.deleteByRoute(routeId)
}

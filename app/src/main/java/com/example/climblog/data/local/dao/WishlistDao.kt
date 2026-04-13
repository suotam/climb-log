package com.example.climblog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.climblog.data.local.entity.WishlistEntity
import com.example.climblog.data.local.entity.WishlistWithRoute
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    @Transaction
    @Query("SELECT * FROM wishlists ORDER BY priority DESC, addedAt DESC")
    fun getWishlistWithRoutes(): Flow<List<WishlistWithRoute>>

    @Query("SELECT * FROM wishlists WHERE routeId = :routeId LIMIT 1")
    fun getByRoute(routeId: Long): Flow<WishlistEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: WishlistEntity): Long

    @Update
    suspend fun update(entry: WishlistEntity)

    @Delete
    suspend fun delete(entry: WishlistEntity)

    @Query("DELETE FROM wishlists WHERE routeId = :routeId")
    suspend fun deleteByRoute(routeId: Long)
}

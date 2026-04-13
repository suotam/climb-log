package com.example.climblog.data.local.dao

import androidx.room.*
import com.example.climblog.data.local.entity.AscentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AscentDao {
    @Query("SELECT * FROM ascents WHERE routeId = :routeId ORDER BY date DESC")
    fun getAscentsByRoute(routeId: Long): Flow<List<AscentEntity>>

    @Query("SELECT * FROM ascents ORDER BY date DESC")
    fun getAllAscents(): Flow<List<AscentEntity>>

    @Query("SELECT * FROM ascents WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getAscentsByDateRange(startDate: Long, endDate: Long): Flow<List<AscentEntity>>

    @Query("SELECT * FROM ascents WHERE id = :id")
    suspend fun getAscentById(id: Long): AscentEntity?

    @Query("SELECT COUNT(*) FROM ascents WHERE routeId = :routeId")
    suspend fun countByRoute(routeId: Long): Int

    @Query("SELECT COUNT(*) FROM ascents")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(DISTINCT routeId) FROM ascents WHERE style NOT IN ('ATTEMPT', 'PROJECT')")
    suspend fun countSentRoutes(): Int

    @Query("SELECT style, COUNT(*) as cnt FROM ascents GROUP BY style")
    suspend fun getAscentCountByStyle(): List<StyleCount>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ascent: AscentEntity): Long

    @Update
    suspend fun update(ascent: AscentEntity)

    @Delete
    suspend fun delete(ascent: AscentEntity)
}

data class StyleCount(val style: String, val cnt: Int)

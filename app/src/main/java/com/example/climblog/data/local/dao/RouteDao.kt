package com.example.climblog.data.local.dao

import androidx.room.*
import com.example.climblog.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes WHERE sectorId = :sectorId ORDER BY `order` ASC, name ASC")
    fun getRoutesBySector(sectorId: Long): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getRouteById(id: Long): RouteEntity?

    @Query("SELECT * FROM routes WHERE id IN (:ids)")
    suspend fun getRoutesByIds(ids: List<Long>): List<RouteEntity>

    @Query("SELECT COUNT(*) FROM routes WHERE sectorId = :sectorId")
    suspend fun countBySector(sectorId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(routes: List<RouteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Delete
    suspend fun delete(route: RouteEntity)

    @Query("DELETE FROM routes WHERE remoteId LIKE 'chs-%'")
    suspend fun deleteChsRoutes()
}

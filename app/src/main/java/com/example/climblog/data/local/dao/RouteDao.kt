package com.example.climblog.data.local.dao

import androidx.room.*
import com.example.climblog.data.local.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

data class RouteWithContext(
    val routeId: Long,
    val routeName: String,
    val grade: String,
    val sectorId: Long,
    val sectorName: String,
    val areaName: String
)

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes WHERE sectorId = :sectorId ORDER BY `order` ASC, name ASC")
    fun getRoutesBySector(sectorId: Long): Flow<List<RouteEntity>>

    @Query("""
        SELECT r.id as routeId, r.name as routeName, r.grade, r.sectorId, s.name as sectorName, a.name as areaName
        FROM routes r
        JOIN sectors s ON r.sectorId = s.id
        JOIN areas a ON s.areaId = a.id
        WHERE r.name LIKE :pattern OR r.grade LIKE :pattern
        ORDER BY r.name ASC LIMIT 100
    """)
    fun searchRoutes(pattern: String): Flow<List<RouteWithContext>>

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

    @Query("UPDATE routes SET lezecId = :lezecId WHERE remoteId = :remoteId")
    suspend fun updateLezecId(remoteId: String, lezecId: Int?)
}

package com.example.climblog.data.local.dao

import androidx.room.*
import com.example.climblog.data.local.entity.SectorEntity
import kotlinx.coroutines.flow.Flow

data class SectorWithArea(
    val sectorId: Long,
    val sectorName: String,
    val areaId: Long,
    val areaName: String
)

@Dao
interface SectorDao {
    @Query("SELECT * FROM sectors WHERE areaId = :areaId ORDER BY `order` ASC, name ASC")
    fun getSectorsByArea(areaId: Long): Flow<List<SectorEntity>>

    @Query("""
        SELECT s.id as sectorId, s.name as sectorName, s.areaId, a.name as areaName
        FROM sectors s JOIN areas a ON s.areaId = a.id
        WHERE s.name LIKE :pattern
        ORDER BY s.name ASC LIMIT 60
    """)
    fun searchSectors(pattern: String): Flow<List<SectorWithArea>>

    @Query("SELECT * FROM sectors WHERE id = :id")
    suspend fun getSectorById(id: Long): SectorEntity?

    @Query("SELECT COUNT(*) FROM sectors WHERE areaId = :areaId")
    suspend fun countByArea(areaId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sectors: List<SectorEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sector: SectorEntity): Long

    @Update
    suspend fun update(sector: SectorEntity)

    @Delete
    suspend fun delete(sector: SectorEntity)

    @Query("DELETE FROM sectors WHERE remoteId LIKE 'chs-%'")
    suspend fun deleteChsSectors()
}

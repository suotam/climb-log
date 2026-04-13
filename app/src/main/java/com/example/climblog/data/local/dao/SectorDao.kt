package com.example.climblog.data.local.dao

import androidx.room.*
import com.example.climblog.data.local.entity.SectorEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SectorDao {
    @Query("SELECT * FROM sectors WHERE areaId = :areaId ORDER BY `order` ASC, name ASC")
    fun getSectorsByArea(areaId: Long): Flow<List<SectorEntity>>

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

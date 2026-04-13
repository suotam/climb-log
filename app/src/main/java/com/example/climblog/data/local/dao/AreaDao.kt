package com.example.climblog.data.local.dao

import androidx.room.*
import com.example.climblog.data.local.entity.AreaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AreaDao {
    @Query("SELECT * FROM areas ORDER BY name ASC")
    fun getAllAreas(): Flow<List<AreaEntity>>

    @Query("SELECT * FROM areas WHERE id = :id")
    suspend fun getAreaById(id: Long): AreaEntity?

    @Query("SELECT COUNT(*) FROM areas")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM areas WHERE name = :name")
    suspend fun countByName(name: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(areas: List<AreaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(area: AreaEntity): Long

    @Update
    suspend fun update(area: AreaEntity)

    @Delete
    suspend fun delete(area: AreaEntity)

    @Query("DELETE FROM areas WHERE remoteId LIKE 'chs-%'")
    suspend fun deleteChsAreas()
}

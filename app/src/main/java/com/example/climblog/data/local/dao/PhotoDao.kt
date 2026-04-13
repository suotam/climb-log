package com.example.climblog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.climblog.data.local.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE areaId = :areaId ORDER BY takenAt DESC")
    fun getPhotosByArea(areaId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE routeId = :routeId ORDER BY takenAt DESC")
    fun getPhotosByRoute(routeId: Long): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE ascentId = :ascentId ORDER BY takenAt DESC")
    fun getPhotosByAscent(ascentId: Long): Flow<List<PhotoEntity>>

    @Query("""
        SELECT p.* FROM photos p
        INNER JOIN ascents a ON p.ascentId = a.id
        WHERE a.date >= :dayStart AND a.date <= :dayEnd
        ORDER BY p.takenAt DESC
    """)
    fun getPhotosByDay(dayStart: Long, dayEnd: Long): Flow<List<PhotoEntity>>

    @Insert
    suspend fun insert(photo: PhotoEntity): Long

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("SELECT * FROM photos WHERE outdoorSessionId = :sessionId ORDER BY takenAt DESC")
    fun getPhotosByOutdoorSession(sessionId: Long): Flow<List<PhotoEntity>>

    @Query("DELETE FROM photos WHERE id = :id")
    suspend fun deleteById(id: Long)
}

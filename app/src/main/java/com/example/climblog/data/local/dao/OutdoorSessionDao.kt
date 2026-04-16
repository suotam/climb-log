package com.example.climblog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.climblog.data.local.entity.OutdoorSessionEntity
import com.example.climblog.data.local.entity.OutdoorSessionWithRoutes
import kotlinx.coroutines.flow.Flow

@Dao
interface OutdoorSessionDao {

    @Transaction
    @Query("SELECT * FROM outdoor_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<OutdoorSessionWithRoutes>>

    @Transaction
    @Query("SELECT * FROM outdoor_sessions WHERE id = :id")
    fun getSessionById(id: Long): Flow<List<OutdoorSessionWithRoutes>>

    @Insert
    suspend fun insertSession(session: OutdoorSessionEntity): Long

    @Update
    suspend fun updateSession(session: OutdoorSessionEntity)

    @Delete
    suspend fun deleteSession(session: OutdoorSessionEntity)
}

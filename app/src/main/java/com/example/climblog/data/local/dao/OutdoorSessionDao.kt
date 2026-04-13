package com.example.climblog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.climblog.data.local.entity.OutdoorSessionEntity
import com.example.climblog.data.local.entity.OutdoorSessionRouteEntity
import com.example.climblog.data.local.entity.OutdoorSessionWithRoutes
import kotlinx.coroutines.flow.Flow

@Dao
interface OutdoorSessionDao {

    @Transaction
    @Query("SELECT * FROM outdoor_sessions ORDER BY date DESC")
    fun getAllSessions(): Flow<List<OutdoorSessionWithRoutes>>

    @Insert
    suspend fun insertSession(session: OutdoorSessionEntity): Long

    @Insert
    suspend fun insertRoutes(routes: List<OutdoorSessionRouteEntity>)

    @Delete
    suspend fun deleteSession(session: OutdoorSessionEntity)
}

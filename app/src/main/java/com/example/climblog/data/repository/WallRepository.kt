package com.example.climblog.data.repository

import com.example.climblog.domain.model.SessionType
import com.example.climblog.domain.model.Wall
import com.example.climblog.domain.model.WallSession
import kotlinx.coroutines.flow.Flow

interface WallRepository {
    fun getAllWalls(): Flow<List<Wall>>
    fun getAllSessions(): Flow<List<WallSession>>
    fun getSessionsForWall(wallId: Long): Flow<List<WallSession>>
    suspend fun saveWall(wall: Wall): Long
    suspend fun deleteWall(wall: Wall)
    suspend fun saveSession(
        wallId: Long,
        date: Long,
        type: SessionType,
        notes: String,
        grades: Map<String, Int>  // grade → count (only count > 0)
    ): Long
    suspend fun deleteSession(session: WallSession)
}

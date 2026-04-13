package com.example.climblog.data.repository

import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.OutdoorSession
import kotlinx.coroutines.flow.Flow

interface OutdoorSessionRepository {
    fun getAllSessions(): Flow<List<OutdoorSession>>
    suspend fun saveSession(
        date: Long,
        areaId: Long?,
        customCragName: String?,
        notes: String,
        routes: List<Pair<Long, AscentStyle>>,
        photoUris: List<String>
    )
    suspend fun deleteSession(session: OutdoorSession)
}

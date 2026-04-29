package com.example.climblog.data.repository

import com.example.climblog.data.local.dao.AscentWithGrade
import com.example.climblog.domain.model.Ascent
import kotlinx.coroutines.flow.Flow

interface AscentRepository {
    fun getAscentsByRoute(routeId: Long): Flow<List<Ascent>>
    fun getAllAscents(): Flow<List<Ascent>>
    fun getAscentsByDateRange(startDate: Long, endDate: Long): Flow<List<Ascent>>
    fun getSentAscentsWithGrade(): Flow<List<AscentWithGrade>>
    suspend fun getAscentById(id: Long): Ascent?
    suspend fun saveAscent(ascent: Ascent): Long
    suspend fun updateAscent(ascent: Ascent)
    suspend fun deleteAscent(ascent: Ascent)
    suspend fun totalAscents(): Int
    suspend fun totalSentRoutes(): Int
}

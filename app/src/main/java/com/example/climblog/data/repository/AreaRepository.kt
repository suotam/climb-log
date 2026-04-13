package com.example.climblog.data.repository

import com.example.climblog.domain.model.Area
import com.example.climblog.domain.model.Sector
import kotlinx.coroutines.flow.Flow

interface AreaRepository {
    fun getAllAreas(): Flow<List<Area>>
    suspend fun getAreaById(id: Long): Area?
    fun getSectorsByArea(areaId: Long): Flow<List<Sector>>
    suspend fun getSectorById(id: Long): Sector?
}

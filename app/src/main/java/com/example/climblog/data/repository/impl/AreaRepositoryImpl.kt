package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.SectorDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.repository.AreaRepository
import com.example.climblog.domain.model.Area
import com.example.climblog.domain.model.Sector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AreaRepositoryImpl @Inject constructor(
    private val areaDao: AreaDao,
    private val sectorDao: SectorDao
) : AreaRepository {

    override fun getAllAreas(): Flow<List<Area>> =
        areaDao.getAllAreas().map { list -> list.map { it.toDomain() } }

    override suspend fun getAreaById(id: Long): Area? =
        areaDao.getAreaById(id)?.toDomain()

    override fun getSectorsByArea(areaId: Long): Flow<List<Sector>> =
        sectorDao.getSectorsByArea(areaId).map { list -> list.map { it.toDomain() } }

    override suspend fun getSectorById(id: Long): Sector? =
        sectorDao.getSectorById(id)?.toDomain()
}

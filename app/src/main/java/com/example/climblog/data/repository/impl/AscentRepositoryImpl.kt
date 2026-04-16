package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.AscentDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.local.entity.toEntity
import com.example.climblog.data.repository.AscentRepository
import com.example.climblog.domain.model.Ascent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AscentRepositoryImpl @Inject constructor(
    private val ascentDao: AscentDao
) : AscentRepository {

    override fun getAscentsByRoute(routeId: Long): Flow<List<Ascent>> =
        ascentDao.getAscentsByRoute(routeId).map { list -> list.map { it.toDomain() } }

    override fun getAllAscents(): Flow<List<Ascent>> =
        ascentDao.getAllAscents().map { list -> list.map { it.toDomain() } }

    override fun getAscentsByDateRange(startDate: Long, endDate: Long): Flow<List<Ascent>> =
        ascentDao.getAscentsByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override suspend fun getAscentById(id: Long): Ascent? =
        ascentDao.getAscentById(id)?.toDomain()

    override suspend fun saveAscent(ascent: Ascent): Long =
        ascentDao.insert(ascent.toEntity())

    override suspend fun updateAscent(ascent: Ascent) =
        ascentDao.update(ascent.toEntity())

    override suspend fun deleteAscent(ascent: Ascent) =
        ascentDao.delete(ascent.toEntity())

    override suspend fun totalAscents(): Int = ascentDao.totalCount()

    override suspend fun totalSentRoutes(): Int = ascentDao.countSentRoutes()
}

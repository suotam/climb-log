package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.local.entity.toEntity
import com.example.climblog.data.repository.RouteRepository
import com.example.climblog.domain.model.Route
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RouteRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao
) : RouteRepository {

    override fun getRoutesBySector(sectorId: Long): Flow<List<Route>> =
        routeDao.getRoutesBySector(sectorId).map { list -> list.map { it.toDomain() } }

    override suspend fun getRouteById(id: Long): Route? =
        routeDao.getRouteById(id)?.toDomain()

    override suspend fun getRoutesByIds(ids: List<Long>): List<Route> =
        if (ids.isEmpty()) emptyList() else routeDao.getRoutesByIds(ids).map { it.toDomain() }

    override suspend fun saveRoute(route: Route): Long =
        routeDao.insert(route.toEntity())

    override suspend fun updateRoute(route: Route) =
        routeDao.update(route.toEntity())

    override suspend fun deleteRoute(route: Route) =
        routeDao.delete(route.toEntity())
}

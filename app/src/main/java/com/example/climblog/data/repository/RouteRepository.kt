package com.example.climblog.data.repository

import com.example.climblog.domain.model.Route
import kotlinx.coroutines.flow.Flow

interface RouteRepository {
    fun getRoutesBySector(sectorId: Long): Flow<List<Route>>
    suspend fun getRouteById(id: Long): Route?
    suspend fun getRoutesByIds(ids: List<Long>): List<Route>
    suspend fun saveRoute(route: Route): Long
    suspend fun updateRoute(route: Route)
    suspend fun deleteRoute(route: Route)
}

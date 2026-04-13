package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.OutdoorSessionDao
import com.example.climblog.data.local.dao.PhotoDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.entity.OutdoorSessionEntity
import com.example.climblog.data.local.entity.OutdoorSessionRouteEntity
import com.example.climblog.data.local.entity.PhotoEntity
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.repository.OutdoorSessionRepository
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.OutdoorSession
import com.example.climblog.domain.model.OutdoorSessionRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutdoorSessionRepositoryImpl @Inject constructor(
    private val dao: OutdoorSessionDao,
    private val areaDao: AreaDao,
    private val routeDao: RouteDao,
    private val photoDao: PhotoDao
) : OutdoorSessionRepository {

    override fun getAllSessions(): Flow<List<OutdoorSession>> =
        dao.getAllSessions().map { sessions ->
            sessions.map { sessionWithRoutes ->
                val areaName = sessionWithRoutes.session.areaId?.let { areaDao.getAreaById(it)?.name }
                val routeDetails = sessionWithRoutes.routes.map { routeEntity ->
                    val route = routeDao.getRouteById(routeEntity.routeId)
                    OutdoorSessionRoute(
                        id = routeEntity.id,
                        sessionId = routeEntity.sessionId,
                        routeId = routeEntity.routeId,
                        routeName = route?.name ?: "?",
                        grade = route?.grade ?: "",
                        style = AscentStyle.valueOf(routeEntity.style)
                    )
                }
                sessionWithRoutes.toDomain(areaName, routeDetails)
            }
        }

    override suspend fun saveSession(
        date: Long,
        areaId: Long?,
        customCragName: String?,
        notes: String,
        routes: List<Pair<Long, AscentStyle>>,
        photoUris: List<String>
    ) {
        val sessionId = dao.insertSession(
            OutdoorSessionEntity(
                date = date,
                areaId = areaId,
                customCragName = customCragName,
                notes = notes
            )
        )
        if (routes.isNotEmpty()) {
            dao.insertRoutes(routes.map { (routeId, style) ->
                OutdoorSessionRouteEntity(
                    sessionId = sessionId,
                    routeId = routeId,
                    style = style.name
                )
            })
        }
        photoUris.forEach { uri ->
            photoDao.insert(PhotoEntity(outdoorSessionId = sessionId, uri = uri))
        }
    }

    override suspend fun deleteSession(session: OutdoorSession) {
        dao.deleteSession(
            OutdoorSessionEntity(
                id = session.id,
                date = session.date,
                areaId = session.areaId,
                customCragName = session.customCragName,
                notes = session.notes
            )
        )
    }
}

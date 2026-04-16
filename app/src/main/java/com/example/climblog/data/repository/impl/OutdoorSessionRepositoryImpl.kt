package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.AreaDao
import com.example.climblog.data.local.dao.AscentDao
import com.example.climblog.data.local.dao.OutdoorSessionDao
import com.example.climblog.data.local.dao.PhotoDao
import com.example.climblog.data.local.dao.RouteDao
import com.example.climblog.data.local.entity.AscentEntity
import com.example.climblog.data.local.entity.OutdoorSessionEntity
import com.example.climblog.data.local.entity.PhotoEntity
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.repository.OutdoorSessionRepository
import com.example.climblog.domain.model.AscentStyle
import com.example.climblog.domain.model.OutdoorSession
import com.example.climblog.domain.model.OutdoorSessionRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OutdoorSessionRepositoryImpl @Inject constructor(
    private val dao: OutdoorSessionDao,
    private val areaDao: AreaDao,
    private val routeDao: RouteDao,
    private val ascentDao: AscentDao,
    private val photoDao: PhotoDao
) : OutdoorSessionRepository {

    override fun getSessionById(id: Long): Flow<OutdoorSession?> =
        dao.getSessionById(id).map { list ->
            list.firstOrNull()?.let { sessionWithRoutes ->
                val areaName = sessionWithRoutes.session.areaId?.let { areaDao.getAreaById(it)?.name }
                val routeDetails = sessionWithRoutes.ascents.map { ascentEntity ->
                    val route = routeDao.getRouteById(ascentEntity.routeId)
                    OutdoorSessionRoute(
                        ascentId = ascentEntity.id,
                        sessionId = sessionWithRoutes.session.id,
                        routeId = ascentEntity.routeId,
                        routeName = route?.name ?: "?",
                        grade = route?.grade ?: "",
                        style = AscentStyle.valueOf(ascentEntity.style)
                    )
                }
                sessionWithRoutes.toDomain(areaName, routeDetails)
            }
        }

    override suspend fun updateNotes(sessionId: Long, notes: String) {
        val existing = dao.getSessionById(sessionId).first().firstOrNull()?.session ?: return
        dao.updateSession(existing.copy(notes = notes))
    }

    override fun getAllSessions(): Flow<List<OutdoorSession>> =
        dao.getAllSessions().map { sessions ->
            sessions.map { sessionWithRoutes ->
                val areaName = sessionWithRoutes.session.areaId?.let { areaDao.getAreaById(it)?.name }
                val routeDetails = sessionWithRoutes.ascents.map { ascentEntity ->
                    val route = routeDao.getRouteById(ascentEntity.routeId)
                    OutdoorSessionRoute(
                        ascentId = ascentEntity.id,
                        sessionId = sessionWithRoutes.session.id,
                        routeId = ascentEntity.routeId,
                        routeName = route?.name ?: "?",
                        grade = route?.grade ?: "",
                        style = AscentStyle.valueOf(ascentEntity.style)
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
        routes.forEach { (routeId, style) ->
            ascentDao.insert(
                AscentEntity(
                    routeId = routeId,
                    date = date,
                    style = style.name,
                    outdoorSessionId = sessionId
                )
            )
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

package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.WallDao
import com.example.climblog.data.local.entity.WallEntity
import com.example.climblog.data.local.entity.WallGradeEntryEntity
import com.example.climblog.data.local.entity.WallSessionEntity
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.local.entity.toEntity
import com.example.climblog.data.repository.WallRepository
import com.example.climblog.domain.model.SessionType
import com.example.climblog.domain.model.Wall
import com.example.climblog.domain.model.WallSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallRepositoryImpl @Inject constructor(
    private val wallDao: WallDao
) : WallRepository {

    override fun getAllWalls(): Flow<List<Wall>> =
        wallDao.getAllWalls().map { it.map { e -> e.toDomain() } }

    override fun getAllSessions(): Flow<List<WallSession>> =
        wallDao.getAllSessionsWithGrades().map { it.map { e -> e.toDomain() } }

    override fun getSessionsForWall(wallId: Long): Flow<List<WallSession>> =
        wallDao.getSessionsForWall(wallId).map { it.map { e -> e.toDomain() } }

    override suspend fun saveWall(wall: Wall): Long =
        wallDao.insertWall(wall.toEntity())

    override suspend fun deleteWall(wall: Wall) =
        wallDao.deleteWall(wall.toEntity())

    override suspend fun saveSession(
        wallId: Long,
        date: Long,
        type: SessionType,
        notes: String,
        grades: Map<String, Int>
    ): Long {
        val sessionId = wallDao.insertSession(
            WallSessionEntity(wallId = wallId, date = date, type = type.name, notes = notes)
        )
        val gradeEntities = grades
            .filter { it.value > 0 }
            .map { (grade, count) -> WallGradeEntryEntity(sessionId = sessionId, grade = grade, count = count) }
        if (gradeEntities.isNotEmpty()) {
            wallDao.insertGradeEntries(gradeEntities)
        }
        return sessionId
    }

    override suspend fun deleteSession(session: WallSession) {
        wallDao.deleteSession(WallSessionEntity(
            id = session.id,
            wallId = session.wallId,
            date = session.date,
            type = session.type.name,
            notes = session.notes
        ))
    }
}

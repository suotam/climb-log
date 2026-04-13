package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.PhotoDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.local.entity.toEntity
import com.example.climblog.data.repository.PhotoRepository
import com.example.climblog.domain.model.Photo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepositoryImpl @Inject constructor(
    private val photoDao: PhotoDao
) : PhotoRepository {

    override fun getPhotosByArea(areaId: Long): Flow<List<Photo>> =
        photoDao.getPhotosByArea(areaId).map { it.map { e -> e.toDomain() } }

    override fun getPhotosByRoute(routeId: Long): Flow<List<Photo>> =
        photoDao.getPhotosByRoute(routeId).map { it.map { e -> e.toDomain() } }

    override fun getPhotosByAscent(ascentId: Long): Flow<List<Photo>> =
        photoDao.getPhotosByAscent(ascentId).map { it.map { e -> e.toDomain() } }

    override fun getPhotosByDay(dayStart: Long, dayEnd: Long): Flow<List<Photo>> =
        photoDao.getPhotosByDay(dayStart, dayEnd).map { it.map { e -> e.toDomain() } }

    override fun getPhotosByOutdoorSession(sessionId: Long): Flow<List<Photo>> =
        photoDao.getPhotosByOutdoorSession(sessionId).map { it.map { e -> e.toDomain() } }

    override suspend fun savePhoto(photo: Photo): Long =
        photoDao.insert(photo.toEntity())

    override suspend fun deletePhoto(photo: Photo) =
        photoDao.delete(photo.toEntity())

    override suspend fun deletePhotoById(id: Long) =
        photoDao.deleteById(id)
}

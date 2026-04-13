package com.example.climblog.data.repository

import com.example.climblog.domain.model.Photo
import kotlinx.coroutines.flow.Flow

interface PhotoRepository {
    fun getPhotosByArea(areaId: Long): Flow<List<Photo>>
    fun getPhotosByRoute(routeId: Long): Flow<List<Photo>>
    fun getPhotosByAscent(ascentId: Long): Flow<List<Photo>>
    fun getPhotosByDay(dayStart: Long, dayEnd: Long): Flow<List<Photo>>
    fun getPhotosByOutdoorSession(sessionId: Long): Flow<List<Photo>>
    suspend fun savePhoto(photo: Photo): Long
    suspend fun deletePhoto(photo: Photo)
    suspend fun deletePhotoById(id: Long)
}

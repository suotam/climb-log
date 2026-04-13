package com.example.climblog.data.repository

import com.example.climblog.domain.model.RouteComment
import kotlinx.coroutines.flow.Flow

interface RouteCommentRepository {
    fun getCommentsByRoute(routeId: Long): Flow<List<RouteComment>>
    suspend fun addComment(comment: RouteComment): Long
    suspend fun deleteComment(comment: RouteComment)
}

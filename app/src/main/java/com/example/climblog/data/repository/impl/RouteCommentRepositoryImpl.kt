package com.example.climblog.data.repository.impl

import com.example.climblog.data.local.dao.RouteCommentDao
import com.example.climblog.data.local.entity.toDomain
import com.example.climblog.data.local.entity.toEntity
import com.example.climblog.data.repository.RouteCommentRepository
import com.example.climblog.domain.model.RouteComment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RouteCommentRepositoryImpl @Inject constructor(
    private val commentDao: RouteCommentDao
) : RouteCommentRepository {

    override fun getCommentsByRoute(routeId: Long): Flow<List<RouteComment>> =
        commentDao.getCommentsByRoute(routeId).map { it.map { e -> e.toDomain() } }

    override suspend fun addComment(comment: RouteComment): Long =
        commentDao.insert(comment.toEntity())

    override suspend fun deleteComment(comment: RouteComment) =
        commentDao.delete(comment.toEntity())
}

package com.example.climblog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.climblog.data.local.entity.RouteCommentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteCommentDao {

    @Query("SELECT * FROM route_comments WHERE routeId = :routeId ORDER BY createdAt ASC")
    fun getCommentsByRoute(routeId: Long): Flow<List<RouteCommentEntity>>

    @Insert
    suspend fun insert(comment: RouteCommentEntity): Long

    @Delete
    suspend fun delete(comment: RouteCommentEntity)

    @Query("DELETE FROM route_comments WHERE id = :id")
    suspend fun deleteById(id: Long)
}

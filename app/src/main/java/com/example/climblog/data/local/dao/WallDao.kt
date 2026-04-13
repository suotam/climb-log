package com.example.climblog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.example.climblog.data.local.entity.WallEntity
import com.example.climblog.data.local.entity.WallGradeEntryEntity
import com.example.climblog.data.local.entity.WallSessionEntity
import com.example.climblog.data.local.entity.WallSessionWithGrades
import kotlinx.coroutines.flow.Flow

@Dao
interface WallDao {

    // ── Walls ─────────────────────────────────────────────────────────────────

    @Query("SELECT * FROM walls ORDER BY name ASC")
    fun getAllWalls(): Flow<List<WallEntity>>

    @Insert
    suspend fun insertWall(wall: WallEntity): Long

    @Delete
    suspend fun deleteWall(wall: WallEntity)

    // ── Sessions ──────────────────────────────────────────────────────────────

    @Transaction
    @Query("SELECT * FROM wall_sessions ORDER BY date DESC")
    fun getAllSessionsWithGrades(): Flow<List<WallSessionWithGrades>>

    @Transaction
    @Query("SELECT * FROM wall_sessions WHERE wallId = :wallId ORDER BY date DESC")
    fun getSessionsForWall(wallId: Long): Flow<List<WallSessionWithGrades>>

    @Insert
    suspend fun insertSession(session: WallSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: WallSessionEntity)

    // ── Grade entries ─────────────────────────────────────────────────────────

    @Insert
    suspend fun insertGradeEntries(grades: List<WallGradeEntryEntity>)

    @Query("DELETE FROM wall_grade_entries WHERE sessionId = :sessionId")
    suspend fun deleteGradesForSession(sessionId: Long)

    @Insert
    suspend fun insertWalls(walls: List<WallEntity>)

    // ── Stats helpers ─────────────────────────────────────────────────────────

    @Query("SELECT COUNT(*) FROM walls WHERE isPreset = 1")
    suspend fun countPresets(): Int

    @Query("SELECT COUNT(*) FROM wall_sessions")
    suspend fun totalSessions(): Int

    @Query("SELECT SUM(count) FROM wall_grade_entries")
    suspend fun totalRoutesClimbed(): Int?
}

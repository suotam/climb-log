package com.example.climblog.domain.model

data class Wall(
    val id: Long = 0,
    val name: String,
    val city: String = "",
    val description: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isPreset: Boolean = false
)

data class WallGradeEntry(
    val id: Long = 0,
    val sessionId: Long,
    val grade: String,
    val count: Int
)

data class WallSession(
    val id: Long = 0,
    val wallId: Long,
    val wallName: String = "",
    val date: Long,
    val type: SessionType,
    val notes: String = "",
    val grades: List<WallGradeEntry> = emptyList()
) {
    val totalRoutes: Int get() = grades.sumOf { it.count }
    val gradeSummary: String get() = grades
        .filter { it.count > 0 }
        .joinToString(", ") { "${it.grade}×${it.count}" }
        .ifBlank { "—" }
}

package com.example.climblog.domain.model

enum class SessionType(val label: String) {
    BOULDER("Boulder"),
    ROPE("Lano")
}

enum class RopeGradeSystem(val label: String) {
    FRENCH("French"),
    UIAA("UIAA")
}

enum class BoulderGradeSystem(val label: String) {
    FRENCH("French"),
    V_SCALE("V")
}

val BOULDER_GRADES_FRENCH = listOf(
    "3", "4", "5", "5+",
    "6A", "6A+", "6B", "6B+", "6C", "6C+",
    "7A", "7A+", "7B", "7B+", "7C", "7C+",
    "8A", "8A+", "8B", "8B+", "8C"
)

val BOULDER_GRADES_V = listOf(
    "V0-", "V0", "V1", "V2", "V3", "V4", "V5",
    "V6", "V7", "V8", "V9", "V10", "V11", "V12", "V13"
)

val ROPE_GRADES_FRENCH = listOf(
    "5a", "5b", "5c", "5c+",
    "6a", "6a+", "6b", "6b+", "6c", "6c+",
    "7a", "7a+", "7b", "7b+", "7c", "7c+",
    "8a", "8a+", "8b", "8b+", "8c"
)

val ROPE_GRADES_UIAA = listOf(
    "IV", "IV+",
    "V-", "V", "V+",
    "VI-", "VI", "VI+",
    "VII-", "VII", "VII+",
    "VIII-", "VIII", "VIII+",
    "IX-", "IX", "IX+",
    "X-", "X", "X+",
    "XI-", "XI", "XI+"
)

fun gradesForType(
    type: SessionType,
    ropeSystem: RopeGradeSystem = RopeGradeSystem.FRENCH,
    boulderSystem: BoulderGradeSystem = BoulderGradeSystem.FRENCH
) = when (type) {
    SessionType.BOULDER -> if (boulderSystem == BoulderGradeSystem.FRENCH) BOULDER_GRADES_FRENCH else BOULDER_GRADES_V
    SessionType.ROPE -> if (ropeSystem == RopeGradeSystem.FRENCH) ROPE_GRADES_FRENCH else ROPE_GRADES_UIAA
}

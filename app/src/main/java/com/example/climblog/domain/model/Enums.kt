package com.example.climblog.domain.model

enum class GradeSystem(val label: String) {
    FRENCH("French"),
    UIAA("UIAA"),
    YDS("YDS")
}

enum class RouteType(val label: String) {
    SPORT("Sport"),
    TRAD("Trad"),
    BOULDER("Boulder"),
    DWS("DWS")
}

enum class AscentStyle(val label: String) {
    ONSIGHT("Onsight"),
    FLASH("Flash"),
    REDPOINT("Redpoint"),
    TOPROPE("Top rope"),
    ATTEMPT("Attempt"),
    PROJECT("Project")
}

/** Priorita stylu pro zobrazení nejlepšího přelezu (nižší = lepší). */
val AscentStyle.priority: Int get() = when (this) {
    AscentStyle.ONSIGHT  -> 0
    AscentStyle.FLASH    -> 1
    AscentStyle.REDPOINT -> 2
    AscentStyle.TOPROPE  -> 3
    AscentStyle.ATTEMPT  -> 4
    AscentStyle.PROJECT  -> 5
}

enum class SyncStatus { LOCAL, SYNCED, DIRTY }

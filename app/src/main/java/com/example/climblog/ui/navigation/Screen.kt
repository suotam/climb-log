package com.example.climblog.ui.navigation

sealed class Screen(val route: String) {
    // ── Explore stack ──────────────────────────────────────────────────────
    object AreaList : Screen("areas")

    object AreaDetail : Screen("areas/{areaId}") {
        fun createRoute(areaId: Long) = "areas/$areaId"
    }

    object SectorList : Screen("areas/{areaId}/sectors") {
        fun createRoute(areaId: Long) = "areas/$areaId/sectors"
    }

    object RouteList : Screen("sectors/{sectorId}/routes") {
        fun createRoute(sectorId: Long) = "sectors/$sectorId/routes"
    }

    object RouteDetail : Screen("routes/{routeId}") {
        fun createRoute(routeId: Long) = "routes/$routeId"
    }

    object LogAscent : Screen("routes/{routeId}/log?ascentId={ascentId}") {
        fun createRoute(routeId: Long, ascentId: Long? = null) =
            if (ascentId != null) "routes/$routeId/log?ascentId=$ascentId"
            else "routes/$routeId/log?ascentId=-1"
    }

    object BulkLogAscent : Screen("routes/bulk-log?routeIds={routeIds}") {
        fun createRoute(routeIds: List<Long>) = "routes/bulk-log?routeIds=${routeIds.joinToString(",")}"
    }

    object AddWallSession : Screen("walls/add")
    object AddOutdoorSession : Screen("outdoor/add")

    object OutdoorSessionDetail : Screen("outdoor/{sessionId}") {
        fun createRoute(sessionId: Long) = "outdoor/$sessionId"
    }

    object EditRoute : Screen("routes/edit?sectorId={sectorId}&routeId={routeId}") {
        fun createRoute(sectorId: Long) = "routes/edit?sectorId=$sectorId&routeId=-1"
        fun editRoute(routeId: Long) = "routes/edit?sectorId=-1&routeId=$routeId"
    }

    // ── Bottom nav tabs ────────────────────────────────────────────────────
    object Logbook : Screen("logbook")
    object Stats : Screen("stats")
    object Calendar : Screen("calendar")
    object Wishlist : Screen("wishlist")
}

val bottomNavRoutes = setOf(
    Screen.AreaList.route,
    Screen.Logbook.route,
    Screen.Stats.route,
    Screen.Calendar.route,
    Screen.Wishlist.route
)

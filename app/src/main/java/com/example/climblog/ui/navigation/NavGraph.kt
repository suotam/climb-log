package com.example.climblog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.climblog.ui.screen.areas.AreaDetailScreen
import com.example.climblog.ui.screen.areas.AreaListScreen
import com.example.climblog.ui.screen.ascent.LogAscentScreen
import com.example.climblog.ui.screen.calendar.CalendarScreen
import com.example.climblog.ui.screen.logbook.LogbookScreen
import com.example.climblog.ui.screen.routes.EditRouteScreen
import com.example.climblog.ui.screen.routes.RouteDetailScreen
import com.example.climblog.ui.screen.routes.RouteListScreen
import com.example.climblog.ui.screen.sectors.SectorListScreen
import com.example.climblog.ui.screen.stats.StatsScreen
import com.example.climblog.ui.screen.outdoor.AddOutdoorSessionScreen
import com.example.climblog.ui.screen.walls.AddWallSessionScreen
import com.example.climblog.ui.screen.wishlist.WishlistScreen

@Composable
fun ClimbLogNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.AreaList.route,
        modifier = modifier
    ) {
        // ── Explore ──────────────────────────────────────────────────────────
        composable(Screen.AreaList.route) {
            AreaListScreen(
                onAreaClick = { areaId ->
                    navController.navigate(Screen.AreaDetail.createRoute(areaId))
                }
            )
        }

        composable(
            route = Screen.AreaDetail.route,
            arguments = listOf(navArgument("areaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val areaId = backStackEntry.arguments!!.getLong("areaId")
            AreaDetailScreen(
                areaId = areaId,
                onSectorsClick = { id -> navController.navigate(Screen.SectorList.createRoute(id)) },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.SectorList.route,
            arguments = listOf(navArgument("areaId") { type = NavType.LongType })
        ) { backStackEntry ->
            val areaId = backStackEntry.arguments!!.getLong("areaId")
            SectorListScreen(
                areaId = areaId,
                onSectorClick = { sectorId -> navController.navigate(Screen.RouteList.createRoute(sectorId)) },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.RouteList.route,
            arguments = listOf(navArgument("sectorId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sectorId = backStackEntry.arguments!!.getLong("sectorId")
            RouteListScreen(
                sectorId = sectorId,
                onRouteClick = { routeId -> navController.navigate(Screen.RouteDetail.createRoute(routeId)) },
                onAddRoute = { navController.navigate(Screen.EditRoute.createRoute(sectorId)) },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.RouteDetail.route,
            arguments = listOf(navArgument("routeId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments!!.getLong("routeId")
            RouteDetailScreen(
                routeId = routeId,
                onLogAscent = { navController.navigate(Screen.LogAscent.createRoute(routeId)) },
                onEditRoute = { navController.navigate(Screen.EditRoute.editRoute(routeId)) },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.EditRoute.route,
            arguments = listOf(
                navArgument("sectorId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("routeId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) {
            EditRouteScreen(
                onSaved = { navController.navigateUp() },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(
            route = Screen.LogAscent.route,
            arguments = listOf(
                navArgument("routeId") { type = NavType.LongType },
                navArgument("ascentId") { type = NavType.LongType; defaultValue = -1L }
            )
        ) { backStackEntry ->
            val routeId = backStackEntry.arguments!!.getLong("routeId")
            val ascentId = backStackEntry.arguments!!.getLong("ascentId").takeIf { it != -1L }
            LogAscentScreen(
                routeId = routeId,
                editAscentId = ascentId,
                onSaved = { navController.navigateUp() },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        // ── Bottom nav tabs ───────────────────────────────────────────────────
        composable(Screen.AddOutdoorSession.route) {
            AddOutdoorSessionScreen(
                onSaved = { navController.navigateUp() },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.AddWallSession.route) {
            AddWallSessionScreen(
                onSaved = { navController.navigateUp() },
                onNavigateUp = { navController.navigateUp() }
            )
        }

        composable(Screen.Logbook.route) {
            LogbookScreen(
                onRouteClick = { routeId -> navController.navigate(Screen.RouteDetail.createRoute(routeId)) },
                onAddWallSession = { navController.navigate(Screen.AddWallSession.route) },
                onAddOutdoorSession = { navController.navigate(Screen.AddOutdoorSession.route) }
            )
        }

        composable(Screen.Stats.route) { StatsScreen() }

        composable(Screen.Calendar.route) {
            CalendarScreen(
                onAscentClick = { routeId -> navController.navigate(Screen.RouteDetail.createRoute(routeId)) }
            )
        }

        composable(Screen.Wishlist.route) {
            WishlistScreen(
                onRouteClick = { routeId -> navController.navigate(Screen.RouteDetail.createRoute(routeId)) }
            )
        }
    }
}

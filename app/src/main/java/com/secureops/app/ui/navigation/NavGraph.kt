package com.secureops.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.secureops.app.ui.screens.dashboard.DashboardScreen
import com.secureops.app.ui.screens.analytics.AnalyticsScreen
import com.secureops.app.ui.screens.settings.SettingsScreen
import com.secureops.app.ui.screens.voice.VoiceScreen
import com.secureops.app.ui.screens.details.BuildDetailsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object Voice : Screen("voice")
    object BuildDetails : Screen("build_details/{pipelineId}") {
        fun createRoute(pipelineId: String) = "build_details/$pipelineId"
    }
}

@Composable
fun SecureOpsNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToBuildDetails = { pipelineId ->
                    navController.navigate(Screen.BuildDetails.createRoute(pipelineId))
                }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }

        composable(Screen.Voice.route) {
            VoiceScreen()
        }

        composable(
            route = Screen.BuildDetails.route,
            arguments = listOf(
                navArgument("pipelineId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val pipelineId = backStackEntry.arguments?.getString("pipelineId") ?: ""
            BuildDetailsScreen(
                pipelineId = pipelineId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

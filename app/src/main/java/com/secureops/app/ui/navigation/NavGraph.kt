package com.secureops.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.secureops.app.ui.screens.aimodels.AIModelsScreen
import com.secureops.app.ui.screens.dashboard.DashboardScreen
import com.secureops.app.ui.screens.analytics.AnalyticsScreen
import com.secureops.app.ui.screens.settings.SettingsScreen
import com.secureops.app.ui.screens.settings.AddAccountScreen
import com.secureops.app.ui.screens.settings.ManageAccountsScreen
import com.secureops.app.ui.screens.voice.VoiceScreen
import com.secureops.app.ui.screens.details.BuildDetailsScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Analytics : Screen("analytics")
    object Settings : Screen("settings")
    object AddAccount : Screen("add_account")
    object ManageAccounts : Screen("manage_accounts")
    object Voice : Screen("voice")
    object AIModels : Screen("ai_models")
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
            SettingsScreen(
                onNavigateToAddAccount = {
                    navController.navigate(Screen.AddAccount.route)
                },
                onNavigateToManageAccounts = {
                    navController.navigate(Screen.ManageAccounts.route)
                },
                onNavigateToAIModels = {
                    navController.navigate(Screen.AIModels.route)
                }
            )
        }

        composable(Screen.AddAccount.route) {
            AddAccountScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ManageAccounts.route) {
            ManageAccountsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAddAccount = {
                    navController.navigate(Screen.AddAccount.route)
                }
            )
        }

        composable(Screen.AIModels.route) {
            AIModelsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
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

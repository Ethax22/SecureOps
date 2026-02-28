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
import com.secureops.app.ui.screens.settings.EditAccountScreen
import com.secureops.app.ui.screens.settings.NotificationSettingsScreen
import com.secureops.app.ui.screens.settings.OfflineSettingsScreen
import com.secureops.app.ui.screens.voice.VoiceScreen
import com.secureops.app.ui.screens.details.BuildDetailsScreen
import com.secureops.app.ui.screens.modelperformance.ModelPerformanceScreen
import com.secureops.app.ui.screens.security.SecurityScreen
import com.secureops.app.ui.screens.benchmark.BenchmarkScreen
import com.secureops.app.ui.screens.about.AboutScreen

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Analytics : Screen("analytics")
    object Security : Screen("security")
    object Settings : Screen("settings")
    object AddAccount : Screen("add_account")
    object ManageAccounts : Screen("manage_accounts")
    object EditAccount : Screen("edit_account/{accountId}") {
        fun createRoute(accountId: String) = "edit_account/$accountId"
    }
    object NotificationSettings : Screen("notification_settings")
    object OfflineSettings : Screen("offline_settings")
    object Voice : Screen("voice")
    object AIModels : Screen("ai_models")
    object BuildDetails : Screen("build_details/{pipelineId}") {
        fun createRoute(pipelineId: String) = "build_details/$pipelineId"
    }
    object ModelPerformance : Screen("model_performance")
    object Benchmark : Screen("benchmark")
    object About : Screen("about")
}

@Composable
fun SecureOpsNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route,
    onDarkModeChanged: (Boolean) -> Unit = {}
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

        composable(Screen.Security.route) {
            SecurityScreen()
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
                },
                onNavigateToNotificationSettings = {
                    navController.navigate(Screen.NotificationSettings.route)
                },
                onNavigateToOfflineSettings = {
                    navController.navigate(Screen.OfflineSettings.route)
                },
                onDarkModeChanged = onDarkModeChanged
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
                },
                onNavigateToEditAccount = { accountId ->
                    navController.navigate(Screen.EditAccount.createRoute(accountId))
                }
            )
        }

        composable(
            route = Screen.EditAccount.route,
            arguments = listOf(
                navArgument("accountId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val accountId = backStackEntry.arguments?.getString("accountId") ?: ""
            EditAccountScreen(
                accountId = accountId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NotificationSettings.route) {
            NotificationSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.OfflineSettings.route) {
            OfflineSettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
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

        composable(Screen.ModelPerformance.route) {
            ModelPerformanceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Benchmark.route) {
            BenchmarkScreen()
        }

        composable(Screen.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
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

package com.winzone.companion.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.winzone.companion.ui.capture.CaptureScreen
import com.winzone.companion.ui.home.HomeScreen
import com.winzone.companion.ui.join.JoinScreen
import com.winzone.companion.ui.login.LoginScreen
import com.winzone.companion.ui.onboarding.OnboardingScreen
import com.winzone.companion.ui.permission.PermissionScreen
import com.winzone.companion.ui.settings.DiagnosticsScreen
import com.winzone.companion.ui.settings.SettingsScreen

object NavRoutes {
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val HOME = "home"
    const val JOIN = "join/{matchId}"
    const val PERMISSION = "permission/{matchId}/{side}"
    const val CAPTURE = "capture/{matchId}/{side}"
    const val SETTINGS = "settings"
    const val DIAGNOSTICS = "diagnostics"

    fun join(matchId: String) = "join/$matchId"
    fun permission(matchId: String, side: String) = "permission/$matchId/$side"
    fun capture(matchId: String, side: String) = "capture/$matchId/$side"
}

@Composable
fun AppNav(
    navController: NavHostController,
    startDestination: String,
    pendingMatchId: String? = null,
    onMatchConsumed: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    val destination = if (!pendingMatchId.isNullOrBlank()) {
                        val match = pendingMatchId
                        onMatchConsumed()
                        NavRoutes.join(match)
                    } else {
                        NavRoutes.HOME
                    }
                    navController.navigate(destination) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.HOME) {
            HomeScreen(
                onJoinMatch = { matchId ->
                    navController.navigate(NavRoutes.join(matchId))
                },
                onOpenSettings = {
                    navController.navigate(NavRoutes.SETTINGS)
                },
                onSignedOut = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.JOIN,
            arguments = listOf(navArgument("matchId") { type = NavType.StringType })
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            JoinScreen(
                onContinueToPermissions = { id, side ->
                    navController.navigate(NavRoutes.permission(id, side))
                }
            )
        }

        composable(
            route = NavRoutes.PERMISSION,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("side") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            val side = backStackEntry.arguments?.getString("side") ?: "a"
            PermissionScreen(
                matchId = matchId,
                side = side,
                onCaptureStarted = {
                    navController.navigate(NavRoutes.capture(matchId, side)) {
                        popUpTo(NavRoutes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = NavRoutes.CAPTURE,
            arguments = listOf(
                navArgument("matchId") { type = NavType.StringType },
                navArgument("side") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val matchId = backStackEntry.arguments?.getString("matchId").orEmpty()
            CaptureScreen(
                matchId = matchId,
                onStopCaptureConfirmed = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenDiagnostics = { navController.navigate(NavRoutes.DIAGNOSTICS) },
                onSignedOut = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.DIAGNOSTICS) {
            DiagnosticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

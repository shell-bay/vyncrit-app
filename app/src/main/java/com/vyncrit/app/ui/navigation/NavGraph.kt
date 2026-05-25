package com.vyncrit.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vyncrit.app.ui.screens.ChatScreen
import com.vyncrit.app.ui.screens.DashboardScreen
import com.vyncrit.app.ui.screens.LoginScreen
import com.vyncrit.app.ui.screens.PreviewScreen
import com.vyncrit.app.ui.screens.RegisterScreen
import com.vyncrit.app.ui.screens.SettingsScreen
import com.vyncrit.app.ui.screens.TerminalScreen
import com.vyncrit.app.viewmodel.AuthViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsState()
    val startDestination = if (authState.isLoggedIn) Screen.Dashboard.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Right,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onProjectClick = { projectId ->
                    navController.navigate(Screen.ProjectEditor.createRoute(projectId))
                },
                onNewProject = {
                    navController.navigate(Screen.Chat.createRoute("new"))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ChatScreen(
                projectId = projectId,
                onNavigateToTerminal = {
                    navController.navigate(Screen.Terminal.createRoute(projectId))
                },
                onNavigateToPreview = {
                    navController.navigate(Screen.Preview.createRoute(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Terminal.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            TerminalScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Preview.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            PreviewScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProjectEditor.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ChatScreen(
                projectId = projectId,
                onNavigateToTerminal = {
                    navController.navigate(Screen.Terminal.createRoute(projectId))
                },
                onNavigateToPreview = {
                    navController.navigate(Screen.Preview.createRoute(projectId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}

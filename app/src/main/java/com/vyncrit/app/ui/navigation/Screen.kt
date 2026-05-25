package com.vyncrit.app.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("auth/login")
    data object Register : Screen("auth/register")
    data object Dashboard : Screen("dashboard")
    data object Settings : Screen("settings")

    data object Chat : Screen("project/{projectId}/chat") {
        fun createRoute(projectId: String) = "project/$projectId/chat"
    }

    data object Terminal : Screen("project/{projectId}/terminal") {
        fun createRoute(projectId: String) = "project/$projectId/terminal"
    }

    data object Preview : Screen("project/{projectId}/preview") {
        fun createRoute(projectId: String) = "project/$projectId/preview"
    }

    data object ProjectEditor : Screen("project/{projectId}") {
        fun createRoute(projectId: String) = "project/$projectId"
    }
}

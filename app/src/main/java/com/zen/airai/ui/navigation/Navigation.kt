package com.zen.airai.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zen.airai.ui.screens.chat.ChatScreen
import com.zen.airai.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Chat : Screen("chat")
    data object Settings : Screen("settings")
}

@Composable
fun AirAINavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}

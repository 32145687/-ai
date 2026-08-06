package com.myai.assistant.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.myai.assistant.ui.screens.chat.ChatScreen
import com.myai.assistant.ui.screens.memory.MemoryScreen
import com.myai.assistant.ui.screens.persona.PersonaScreen
import com.myai.assistant.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object Persona : Screen("persona")
    object Memory : Screen("memory")
}

@Composable
fun AppNavHost(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Chat.route
    ) {
        composable(Screen.Chat.route) {
            ChatScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPersona = { navController.navigate(Screen.Persona.route) },
                onNavigateToMemory = { navController.navigate(Screen.Memory.route) }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Persona.route) {
            PersonaScreen(
                onPersonaSelected = { personaId ->
                    // 切换人格后返回聊天界面
                    navController.popBackStack()
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Memory.route) {
            MemoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

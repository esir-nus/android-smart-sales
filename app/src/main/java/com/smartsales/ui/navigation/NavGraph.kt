package com.smartsales.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smartsales.ui.screens.chat.ChatScreen
import com.smartsales.ui.screens.conversation.ConversationListScreen
import com.smartsales.ui.screens.device.DevicePairingScreen
import com.smartsales.ui.screens.settings.SettingsScreen
import com.smartsales.ui.screens.sync.FileSyncScreen

/**
 * Navigation Graph
 * 
 * Defines all navigation routes and their destinations
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.ConversationList.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        
        // Conversation List Screen (Home)
        composable(route = Screen.ConversationList.route) {
            ConversationListScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversation = {
                    // Create new conversation and navigate to chat
                    navController.navigate(Screen.Chat.createRoute(0L))
                }
            )
        }
        
        // Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            
            ChatScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExportClick = { id ->
                    navController.navigate(Screen.Export.createRoute(id))
                }
            )
        }
        
        // Device Pairing Screen
        composable(route = Screen.DevicePairing.route) {
            DevicePairingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // File Sync Screen
        composable(route = Screen.FileSync.route) {
            FileSyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Settings Screen
        composable(route = Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        // Export Screen (future implementation)
        composable(
            route = Screen.Export.route,
            arguments = listOf(
                navArgument("conversationId") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            
            // TODO: Implement ExportScreen
            // ExportScreen(conversationId = conversationId)
        }
    }
}
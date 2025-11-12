package com.smartsales.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smartsales.ai.ui.AiAssistantScreen
import com.smartsales.ai.ui.AiRoutes
import com.smartsales.ai.ui.history.AiHistoryRoute
import com.smartsales.ai.ui.structured.StructuredChatRoute
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
    startDestination: String = Screen.ConversationList.route,
    conversationListScreen: @Composable (
        onConversationClick: (Long) -> Unit,
        onNewConversation: () -> Unit,
        onOpenAiAssistant: () -> Unit,
    ) -> Unit = { onConversationClick, onNewConversation, onOpenAiAssistant ->
        ConversationListScreen(
            onConversationClick = onConversationClick,
            onNewConversation = onNewConversation,
            onOpenAiAssistant = onOpenAiAssistant,
        )
    },
    chatScreen: @Composable (
        conversationId: Long,
        onNavigateBack: () -> Unit,
        onExportClick: (Long) -> Unit,
    ) -> Unit = { conversationId, onNavigateBack, onExportClick ->
        ChatScreen(
            conversationId = conversationId,
            onNavigateBack = onNavigateBack,
            onExportClick = onExportClick,
        )
    },
    devicePairingScreen: @Composable (onNavigateBack: () -> Unit) -> Unit = { onNavigateBack ->
        DevicePairingScreen(onNavigateBack = onNavigateBack)
    },
    fileSyncScreen: @Composable (onNavigateBack: () -> Unit) -> Unit = { onNavigateBack ->
        FileSyncScreen(onNavigateBack = onNavigateBack)
    },
    settingsScreen: @Composable (onNavigateBack: () -> Unit) -> Unit = { onNavigateBack ->
        SettingsScreen(onNavigateBack = onNavigateBack)
    },
    exportScreen: @Composable (conversationId: Long, onNavigateBack: () -> Unit) -> Unit = { _, _ -> },
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        // Conversation List Screen (Home)
        composable(route = Screen.ConversationList.route) {
            conversationListScreen(
                onConversationClick = { conversationId ->
                    navController.navigate(Screen.Chat.createRoute(conversationId))
                },
                onNewConversation = {
                    // Create new conversation and navigate to chat
                    navController.navigate(Screen.Chat.createRoute(0L))
                },
                onOpenAiAssistant = {
                    navController.navigate(Screen.AiChat.createRoute())
                },
            )
        }

        // Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments =
                listOf(
                    navArgument("conversationId") {
                        type = NavType.LongType
                    },
                ),
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L

            chatScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onExportClick = { id ->
                    navController.navigate(Screen.Export.createRoute(id))
                },
            )
        }

        // Device Pairing Screen
        composable(route = Screen.DevicePairing.route) {
            devicePairingScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // File Sync Screen
        composable(route = Screen.FileSync.route) {
            fileSyncScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // Settings Screen
        composable(route = Screen.Settings.route) {
            settingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        // Export Screen (future implementation)
        composable(
            route = Screen.Export.route,
            arguments =
                listOf(
                    navArgument("conversationId") {
                        type = NavType.LongType
                    },
                ),
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L

            exportScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        // AI Chat
        composable(
            route = Screen.AiChat.route,
            arguments =
                listOf(
                    navArgument(AiRoutes.CHAT_SESSION_ARG) {
                        type = NavType.StringType
                        defaultValue = AiRoutes.NEW_SESSION_ID
                    },
                ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(AiRoutes.CHAT_SESSION_ARG) ?: AiRoutes.NEW_SESSION_ID
            AiAssistantScreen(
                sessionId = sessionId,
                onNavigateHistory = { navController.navigate(Screen.AiHistory.route) },
                onNavigateStructured = { session ->
                    navController.navigate(Screen.AiStructured.createRoute(session))
                },
            )
        }

        // AI Structured Result
        composable(
            route = Screen.AiStructured.route,
            arguments =
                listOf(
                    navArgument(AiRoutes.STRUCTURED_SESSION_ARG) {
                        type = NavType.StringType
                    },
                ),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString(AiRoutes.STRUCTURED_SESSION_ARG).orEmpty()
            StructuredChatRoute(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
            )
        }

        // AI History
        composable(route = Screen.AiHistory.route) {
            AiHistoryRoute(
                onBack = { navController.popBackStack() },
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.AiChat.createRoute(sessionId))
                },
            )
        }
    }
}

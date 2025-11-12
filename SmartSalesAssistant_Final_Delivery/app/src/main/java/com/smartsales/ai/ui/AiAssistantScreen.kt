package com.smartsales.ai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.smartsales.ai.ui.chat.AiChatRoute
import com.smartsales.ai.ui.menu.AiMenuDrawer
import kotlinx.coroutines.launch

@Composable
fun AiAssistantScreen(
    sessionId: String,
    onNavigateHistory: () -> Unit,
    onNavigateStructured: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AiMenuDrawer(
                onHistoryClick = {
                    scope.launch { drawerState.close() }
                    onNavigateHistory()
                },
                onFilesPlaceholderClick = {
                    scope.launch { drawerState.close() }
                },
                onEditorPlaceholderClick = {
                    scope.launch { drawerState.close() }
                },
                onProfileClick = {
                    scope.launch { drawerState.close() }
                },
            )
        },
    ) {
        AiChatRoute(
            sessionId = sessionId,
            onOpenMenu = { scope.launch { drawerState.open() } },
            onNavigateStructured = onNavigateStructured,
        )
    }
}

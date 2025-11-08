package com.smartsales.ui.navigation

/**
 * Navigation Screens
 * 
 * Sealed class defining all app screens
 */
sealed class Screen(val route: String) {
    
    /**
     * Conversation List Screen (Home)
     */
    object ConversationList : Screen("conversation_list")
    
    /**
     * Chat Screen
     * 
     * Args: conversationId (Long)
     */
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: Long) = "chat/$conversationId"
    }
    
    /**
     * Device Pairing Screen
     */
    object DevicePairing : Screen("device_pairing")
    
    /**
     * File Sync Screen
     */
    object FileSync : Screen("file_sync")
    
    /**
     * Settings Screen
     */
    object Settings : Screen("settings")
    
    /**
     * Export Screen
     * 
     * Args: conversationId (Long)
     */
    object Export : Screen("export/{conversationId}") {
        fun createRoute(conversationId: Long) = "export/$conversationId"
    }
    
    companion object {
        /**
         * Get all bottom navigation screens
         */
        @Suppress("unused")
        fun getBottomNavScreens(): List<Screen> {
            return listOf(
                ConversationList,
                DevicePairing,
                FileSync,
                Settings
            )
        }
    }
}

/**
 * Bottom Navigation Items
 */
@Suppress("unused")
sealed class BottomNavItem(
    val screen: Screen,
    val title: String,
    val icon: Int // Resource ID for icon
) {
    object Conversations : BottomNavItem(
        screen = Screen.ConversationList,
        title = "对话",
        icon = android.R.drawable.ic_dialog_info // Replace with actual icon
    )
    
    object Device : BottomNavItem(
        screen = Screen.DevicePairing,
        title = "设备",
        icon = android.R.drawable.ic_btn_speak_now // Replace with actual icon
    )
    
    object Sync : BottomNavItem(
        screen = Screen.FileSync,
        title = "同步",
        icon = android.R.drawable.stat_notify_sync // Replace with actual icon
    )
    
    object Settings : BottomNavItem(
        screen = Screen.Settings,
        title = "设置",
        icon = android.R.drawable.ic_menu_preferences // Replace with actual icon
    )
    
    companion object {
        @Suppress("unused")
        fun getItems(): List<BottomNavItem> {
            return listOf(
                Conversations,
                Device,
                Sync,
                Settings
            )
        }
    }
}

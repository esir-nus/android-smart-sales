package com.smartsales.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.ui.theme.SmartSalesTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavGraphTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun conversationToChatAndExportFlow_navigatesBetweenDestinations() {
        val navController = TestNavHostController(composeRule.activity)

        composeRule.setContent {
            navController.navigatorProvider.addNavigator(ComposeNavigator())

            SmartSalesTheme {
                NavGraph(
                    navController = navController,
                    conversationListScreen = { onConversationClick, onNewConversation ->
                        Column {
                            Button(
                                onClick = { onConversationClick(42L) },
                                modifier = Modifier.testTag("openConversation")
                            ) {
                                Text("Open Conversation")
                            }
                            Button(
                                onClick = onNewConversation,
                                modifier = Modifier.testTag("newConversation")
                            ) {
                                Text("New Conversation")
                            }
                        }
                    },
                    chatScreen = { conversationId, onNavigateBack, onExportClick ->
                        Column {
                            Text(
                                text = "Chat $conversationId",
                                modifier = Modifier.testTag("chatTitle")
                            )
                            Button(
                                onClick = { onExportClick(conversationId) },
                                modifier = Modifier.testTag("exportChat")
                            ) {
                                Text("Export")
                            }
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("backFromChat")
                            ) {
                                Text("Back")
                            }
                        }
                    },
                    devicePairingScreen = { onNavigateBack ->
                        Button(onClick = onNavigateBack) { Text("Device Back") }
                    },
                    fileSyncScreen = { onNavigateBack ->
                        Button(onClick = onNavigateBack) { Text("Sync Back") }
                    },
                    settingsScreen = { onNavigateBack ->
                        Button(onClick = onNavigateBack) { Text("Settings Back") }
                    },
                    exportScreen = { conversationId, onNavigateBack ->
                        Column {
                            Text(
                                text = "Export $conversationId",
                                modifier = Modifier.testTag("exportTitle")
                            )
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("backFromExport")
                            ) {
                                Text("Back")
                            }
                        }
                    }
                )
            }
        }

        composeRule.runOnIdle {
            assertEquals(
                Screen.ConversationList.route,
                navController.currentBackStackEntry?.destination?.route
            )
        }

        composeRule.onNodeWithTag("openConversation").performClick()

        composeRule.runOnIdle {
            assertEquals(
                Screen.Chat.route,
                navController.currentBackStackEntry?.destination?.route
            )
            assertEquals(
                42L,
                navController.currentBackStackEntry?.arguments?.getLong("conversationId")
            )
        }

        composeRule.onNodeWithTag("exportChat").performClick()
        composeRule.onNodeWithTag("exportTitle").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(
                Screen.Export.route,
                navController.currentBackStackEntry?.destination?.route
            )
            assertEquals(
                42L,
                navController.currentBackStackEntry?.arguments?.getLong("conversationId")
            )
        }

        composeRule.onNodeWithTag("backFromExport").performClick()

        composeRule.runOnIdle {
            assertEquals(
                Screen.Chat.route,
                navController.currentBackStackEntry?.destination?.route
            )
        }

        composeRule.onNodeWithTag("backFromChat").performClick()
        composeRule.onNodeWithText("New Conversation").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(
                Screen.ConversationList.route,
                navController.currentBackStackEntry?.destination?.route
            )
        }
    }
}

package com.smartsales.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.ui.theme.SmartSalesTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noConversationsEmptyState_showsMessageAndButton() {
        var clicked = false

        composeTestRule.setContent {
            SmartSalesTheme {
                NoConversationsEmptyState(
                    onCreateConversation = { clicked = true }
                )
            }
        }

        composeTestRule
            .onNodeWithText("还没有对话记录\n点击下方按钮开始新对话")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("开始新对话")
            .assertIsDisplayed()
            .performClick()

        assertTrue(clicked)
    }

    @Test
    fun noDevicesEmptyState_showsScanAction() {
        var clicked = false

        composeTestRule.setContent {
            SmartSalesTheme {
                NoDevicesEmptyState(onStartScan = { clicked = true })
            }
        }

        composeTestRule
            .onNodeWithText("未发现设备\n请确保设备已开启并在附近")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("开始扫描")
            .assertIsDisplayed()
            .performClick()

        assertTrue(clicked)
    }
}

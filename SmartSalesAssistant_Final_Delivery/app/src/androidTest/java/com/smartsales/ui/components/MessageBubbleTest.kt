package com.smartsales.ui.components

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartsales.data.local.entity.MessageEntity
import com.smartsales.data.local.entity.AttachmentInfo
import com.smartsales.ui.theme.SmartSalesTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageBubbleTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun userMessage_showsAttachmentsBadge() {
        val message = MessageEntity.createUserMessage(
            conversationId = 1,
            content = "请查看附件",
            attachments = listOf(
                AttachmentInfo(
                    type = "image",
                    path = "/tmp/image.png",
                    name = "image.png"
                )
            )
        )

        composeTestRule.setContent {
            SmartSalesTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("请查看附件").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 个附件").assertIsDisplayed()
    }

    @Test
    fun assistantMessage_hidesAttachmentBadge() {
        val message = MessageEntity.createAssistantMessage(
            conversationId = 1,
            content = "好的，马上处理。"
        )

        composeTestRule.setContent {
            SmartSalesTheme {
                MessageBubble(message = message)
            }
        }

        composeTestRule.onNodeWithText("好的，马上处理。").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("个附件").assertCountEquals(0)
    }
}

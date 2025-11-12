package com.smartsales.ai.chat.impl

import com.smartsales.ai.chat.AiAttachment
import com.smartsales.ai.chat.AiChatEvent
import com.smartsales.ai.chat.AiChatRequest
import com.smartsales.ai.chat.AiChatService
import com.smartsales.ai.chat.AiTaskMode
import com.smartsales.data.network.dashscope.DashscopeChatClient
import com.smartsales.data.network.dashscope.DashscopeChatPayload
import com.smartsales.data.network.dashscope.DashscopeStreamEvent
import com.smartsales.data.network.model.ChatInput
import com.smartsales.data.network.model.ChatMessage
import com.smartsales.data.network.model.ChatParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dashscope-based implementation of [AiChatService] backed by the official SDK wrapped in
 * [DashscopeChatClient].
 *
 * We convert each AI task mode into a dedicated system prompt so Dashscope/Qwen can
 * stick to the markdown structure expected by the UI spec. Attachments are summarized
 * inline until we wire the official multimodal API.
 */
@Singleton
class DashscopeAiChatService
    @Inject
    constructor(
        private val dashscopeChatClient: DashscopeChatClient,
    ) : AiChatService {
        override fun streamResponse(request: AiChatRequest): Flow<AiChatEvent> {
            val payload = buildChatPayload(request)
            return dashscopeChatClient.streamChat(payload).map { event ->
                when (event) {
                    is DashscopeStreamEvent.Chunk -> AiChatEvent.Chunk(event.markdown)
                    is DashscopeStreamEvent.Completed -> AiChatEvent.Completed(event.usageTokens)
                    is DashscopeStreamEvent.Error -> AiChatEvent.Error(event.throwable)
                }
            }
        }

        private fun buildChatPayload(request: AiChatRequest): DashscopeChatPayload {
            val messages = buildMessages(request)
            return DashscopeChatPayload(
                model = DEFAULT_MODEL,
                input = ChatInput(messages),
                parameters = ChatParameters.streaming(),
            )
        }

        private fun buildMessages(request: AiChatRequest): List<ChatMessage> {
            val system = ChatMessage.system(systemPrompt(request.taskMode))
            val attachmentNotes = attachmentSummary(request.attachments)
            val localeSuffix =
                if (request.metadata.locale.startsWith("zh", ignoreCase = true)) {
                    "\n请使用 Markdown 中文输出。"
                } else {
                    "\nRespond in Markdown (English)."
                }

            val userContent =
                buildString {
                    append(request.prompt.trim())
                    if (attachmentNotes != null) {
                        append("\n\n附件信息：\n")
                        append(attachmentNotes)
                    }
                    append(localeSuffix)
                }

            return listOf(system, ChatMessage.user(userContent))
        }

        private fun systemPrompt(mode: AiTaskMode): String {
            return when (mode) {
                AiTaskMode.AnalyzeCustomer -> "你是一名资深销售运营顾问，请以 Markdown 结构化输出客户画像、渠道、痛点及后续建议。"
                AiTaskMode.GeneratePdf -> "你是一名文档生成助手，请根据输入整理出可直接渲染为 PDF 的 Markdown，包含标题、摘要、要点列表和结论。"
                AiTaskMode.GenerateCsv -> "你是一名数据整理助手，请输出包含字段标题和值的 Markdown 表格，方便导出为 CSV。"
                is AiTaskMode.Custom ->
                    mode.label.ifBlank {
                        "你是销售助手，请用 Markdown 输出结构化结果。"
                    }
            }
        }

        private fun attachmentSummary(attachments: List<AiAttachment>): String? {
            if (attachments.isEmpty()) return null
            return attachments.joinToString(separator = "\n") { attachment ->
                val fileName = attachment.localPath.substringAfterLast('/')
                "${attachment.type.name.lowercase()}: $fileName (${attachment.mimeType})"
            }
        }

        private companion object {
            private const val DEFAULT_MODEL = "qwen-turbo"
        }
    }

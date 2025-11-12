package com.smartsales.ai.chat

import kotlinx.coroutines.flow.Flow

/**
 * Streams DashScope (Qwen) responses while keeping attachments, exports, and
 * task modes explicit, so the UI can make decisions without parsing raw text.
 */
interface AiChatService {
    fun streamResponse(request: AiChatRequest): Flow<AiChatEvent>
}

data class AiChatRequest(
    val sessionId: String,
    val taskMode: AiTaskMode,
    val prompt: String,
    val attachments: List<AiAttachment> = emptyList(),
    val metadata: AiRequestMetadata = AiRequestMetadata(),
)

data class AiAttachment(
    val localPath: String,
    val mimeType: String,
    val type: AiAttachmentType,
    val sizeBytes: Long,
)

enum class AiAttachmentType {
    IMAGE,
    AUDIO,
}

data class AiRequestMetadata(
    val locale: String = "zh-CN",
    val structuredOutputPreferred: Boolean = false,
    val exportTargets: Set<AiExportTarget> = emptySet(),
)

enum class AiExportTarget {
    PDF,
    CSV,
    MIND_MAP,
}

sealed interface AiChatEvent {
    data class Chunk(val markdown: String) : AiChatEvent

    data class Completed(val usageTokenCount: Int) : AiChatEvent

    data class Error(val throwable: Throwable) : AiChatEvent
}

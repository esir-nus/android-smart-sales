package com.smartsales.ai.chat

/**
 * Represents a single turn in a conversation.
 */
sealed interface AiMessage {
    val id: String
    val createdAtMillis: Long

    data class User(
        override val id: String,
        override val createdAtMillis: Long,
        val text: String,
        val attachments: List<AiAttachment>,
        val taskMode: AiTaskMode,
    ) : AiMessage

    data class Assistant(
        override val id: String,
        override val createdAtMillis: Long,
        val markdown: String,
        val exportTargets: Set<AiExportTarget>,
    ) : AiMessage

    data class System(
        override val id: String,
        override val createdAtMillis: Long,
        val instructions: String,
    ) : AiMessage
}

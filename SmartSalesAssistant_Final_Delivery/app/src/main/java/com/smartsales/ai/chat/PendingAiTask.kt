package com.smartsales.ai.chat

/**
 * State holder tying together the UI (chips, uploader) and the chat service.
 */
data class PendingAiTask(
    val mode: AiTaskMode,
    val prompt: String,
    val attachments: List<AiAttachment> = emptyList(),
    val exportTargets: Set<AiExportTarget> = emptySet(),
    val structuredScreenRequested: Boolean = false,
)

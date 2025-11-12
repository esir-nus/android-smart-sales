package com.smartsales.ai.data.local

import com.smartsales.ai.chat.AiAttachment
import com.smartsales.ai.chat.AiAttachmentType
import com.smartsales.ai.chat.AiExportTarget
import com.smartsales.ai.chat.AiMessage
import com.smartsales.ai.chat.AiTaskMode

internal data class StoredAiMessage(
    val type: String,
    val id: String,
    val createdAtMillis: Long,
    val text: String? = null,
    val markdown: String? = null,
    val exportTargets: List<String>? = null,
    val attachments: List<StoredAiAttachment>? = null,
    val taskMode: StoredAiTaskMode? = null,
    val instructions: String? = null,
)

internal data class StoredAiAttachment(
    val localPath: String,
    val mimeType: String,
    val type: String,
    val sizeBytes: Long,
)

internal data class StoredAiTaskMode(
    val type: String,
    val id: String? = null,
    val label: String? = null,
    val structured: Boolean? = null,
    val shouldPersistAsTemplate: Boolean? = null,
)

internal fun AiMessage.toStored(): StoredAiMessage {
    return when (this) {
        is AiMessage.User ->
            StoredAiMessage(
                type = "user",
                id = id,
                createdAtMillis = createdAtMillis,
                text = text,
                attachments = attachments.map { it.toStored() },
                taskMode = taskMode.toStored(),
            )
        is AiMessage.Assistant ->
            StoredAiMessage(
                type = "assistant",
                id = id,
                createdAtMillis = createdAtMillis,
                markdown = markdown,
                exportTargets = exportTargets.map { it.name },
            )
        is AiMessage.System ->
            StoredAiMessage(
                type = "system",
                id = id,
                createdAtMillis = createdAtMillis,
                instructions = instructions,
            )
    }
}

internal fun StoredAiMessage.toDomain(): AiMessage {
    return when (type) {
        "user" ->
            AiMessage.User(
                id = id,
                createdAtMillis = createdAtMillis,
                text = text.orEmpty(),
                attachments = attachments?.map { it.toDomain() } ?: emptyList(),
                taskMode = taskMode?.toDomain() ?: AiTaskMode.AnalyzeCustomer,
            )
        "assistant" ->
            AiMessage.Assistant(
                id = id,
                createdAtMillis = createdAtMillis,
                markdown = markdown.orEmpty(),
                exportTargets =
                    exportTargets?.mapNotNull {
                        runCatching {
                            AiExportTarget.valueOf(
                                it,
                            )
                        }.getOrNull()
                    }?.toSet().orEmpty(),
            )
        else ->
            AiMessage.System(
                id = id,
                createdAtMillis = createdAtMillis,
                instructions = instructions.orEmpty(),
            )
    }
}

private fun AiAttachment.toStored(): StoredAiAttachment {
    return StoredAiAttachment(
        localPath = localPath,
        mimeType = mimeType,
        type = type.name,
        sizeBytes = sizeBytes,
    )
}

private fun StoredAiAttachment.toDomain(): AiAttachment {
    return AiAttachment(
        localPath = localPath,
        mimeType = mimeType,
        type = runCatching { AiAttachmentType.valueOf(type) }.getOrDefault(AiAttachmentType.IMAGE),
        sizeBytes = sizeBytes,
    )
}

private fun AiTaskMode.toStored(): StoredAiTaskMode {
    return when (this) {
        AiTaskMode.AnalyzeCustomer -> StoredAiTaskMode(type = "analyze_customer")
        AiTaskMode.GeneratePdf -> StoredAiTaskMode(type = "generate_pdf")
        AiTaskMode.GenerateCsv -> StoredAiTaskMode(type = "generate_csv")
        is AiTaskMode.Custom ->
            StoredAiTaskMode(
                type = "custom",
                id = id,
                label = label,
                structured = requiresStructuredScreen,
                shouldPersistAsTemplate = shouldPersistAsTemplate,
            )
    }
}

private fun StoredAiTaskMode.toDomain(): AiTaskMode {
    return when (type) {
        "analyze_customer" -> AiTaskMode.AnalyzeCustomer
        "generate_pdf" -> AiTaskMode.GeneratePdf
        "generate_csv" -> AiTaskMode.GenerateCsv
        "custom" ->
            AiTaskMode.Custom(
                id = id ?: "custom",
                shouldPersistAsTemplate = shouldPersistAsTemplate ?: false,
                structured = structured ?: true,
                label = label ?: "自定义",
            )
        else -> AiTaskMode.AnalyzeCustomer
    }
}

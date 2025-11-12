package com.smartsales.ai.media

import com.smartsales.ai.chat.AiAttachment

/**
 * Validates and compresses audio/image files before they are sent to DashScope.
 */
interface MediaPreprocessor {
    suspend fun prepare(rawAttachmentPath: String): AiAttachment
}

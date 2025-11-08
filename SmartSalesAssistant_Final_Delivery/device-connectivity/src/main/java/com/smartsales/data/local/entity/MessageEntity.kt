package com.smartsales.data.local.entity

import androidx.room.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message Entity - Represents a single message in a conversation
 *
 * Features:
 * - Foreign key to conversation (cascading delete)
 * - Role-based messages (user/assistant)
 * - Attachment support (JSON)
 * - Metadata support (JSON)
 */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val conversationId: Long,
    val role: String,                // "user" or "assistant"
    val content: String,             // Text content
    val timestamp: Long,             // Message timestamp (millis)
    val attachments: String? = null, // JSON array: [{"type":"audio","path":"..."}]
    val metadata: String? = null     // JSON object: {"tokens":1234,"model":"qwen-max"}
) {

    companion object {
        // Role constants
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"

        // Limits
        const val MAX_CONTENT_LENGTH = 50000
        const val MAX_ATTACHMENT_SIZE = 10 * 1024 * 1024 // 10MB

        /**
         * Create user message
         */
        fun createUserMessage(
            conversationId: Long,
            content: String,
            attachments: List<AttachmentInfo>? = null
        ): MessageEntity {
            return MessageEntity(
                conversationId = conversationId,
                role = ROLE_USER,
                content = content.take(MAX_CONTENT_LENGTH),
                timestamp = System.currentTimeMillis(),
                attachments = attachments?.let { encodeAttachments(it) },
                metadata = null
            )
        }

        /**
         * Create assistant message
         */
        fun createAssistantMessage(
            conversationId: Long,
            content: String,
            metadata: MessageMetadata? = null
        ): MessageEntity {
            return MessageEntity(
                conversationId = conversationId,
                role = ROLE_ASSISTANT,
                content = content.take(MAX_CONTENT_LENGTH),
                timestamp = System.currentTimeMillis(),
                attachments = null,
                metadata = metadata?.toJson()
            )
        }

        /**
         * Encode attachments to JSON
         */
        private fun encodeAttachments(attachments: List<AttachmentInfo>): String {
            val jsonArray = JSONArray()
            attachments.forEach { attachment ->
                val jsonObject = JSONObject()
                jsonObject.put("type", attachment.type)
                jsonObject.put("path", attachment.path)
                jsonObject.put("name", attachment.name)
                jsonArray.put(jsonObject)
            }
            return jsonArray.toString()
        }
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Check if message is from user
     */
    val isUserMessage: Boolean
        get() = role == ROLE_USER

    /**
     * Check if message is from assistant
     */
    val isAssistantMessage: Boolean
        get() = role == ROLE_ASSISTANT

    /**
     * Check if message has attachments
     */
    val hasAttachments: Boolean
        get() = !attachments.isNullOrBlank()

    /**
     * Check if message has metadata
     */
    val hasMetadata: Boolean
        get() = !metadata.isNullOrBlank()

    /**
     * Get message length
     */
    val contentLength: Int
        get() = content.length

    /**
     * Check if message is long (>1000 chars)
     */
    val isLongMessage: Boolean
        get() = content.length > 1000

    /**
     * Get message age in seconds
     */
    val ageInSeconds: Long
        get() = (System.currentTimeMillis() - timestamp) / 1000

    /**
     * Check if message is recent (less than 1 minute old)
     */
    val isRecent: Boolean
        get() = ageInSeconds < 60

    // ===== PARSING METHODS =====

    /**
     * Parse attachments from JSON
     */
    fun getAttachmentsList(): List<AttachmentInfo> {
        if (attachments.isNullOrBlank()) return emptyList()

        return try {
            val jsonArray = JSONArray(attachments)
            List(jsonArray.length()) { index ->
                val jsonObject = jsonArray.getJSONObject(index)
                AttachmentInfo(
                    type = jsonObject.getString("type"),
                    path = jsonObject.getString("path"),
                    name = jsonObject.optString("name", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parse metadata from JSON
     */
    fun getMetadataObject(): MessageMetadata? {
        if (metadata.isNullOrBlank()) return null

        return try {
            val jsonObject = JSONObject(metadata)
            MessageMetadata(
                tokens = jsonObject.optInt("tokens", 0),
                model = jsonObject.optString("model", ""),
                processingTime = jsonObject.optLong("processingTime", 0)
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Count attachments
     */
    fun getAttachmentCount(): Int {
        return getAttachmentsList().size
    }

    // ===== FORMATTING METHODS =====

    /**
     * Format timestamp
     */
    fun getFormattedTime(pattern: String = "HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Format full date
     */
    fun getFormattedDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Get relative time string
     */
    fun getRelativeTimeString(): String {
        val seconds = ageInSeconds

        return when {
            seconds < 60 -> "Just now"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86400 -> "${seconds / 3600}h ago"
            else -> getFormattedTime()
        }
    }

    /**
     * Get content preview (first N characters)
     */
    fun getContentPreview(maxLength: Int = 100): String {
        return if (content.length <= maxLength) {
            content
        } else {
            content.take(maxLength) + "..."
        }
    }

    // ===== VALIDATION METHODS =====

    /**
     * Validate message data
     */
    fun isValid(): Boolean {
        return content.isNotBlank() &&
                content.length <= MAX_CONTENT_LENGTH &&
                role in listOf(ROLE_USER, ROLE_ASSISTANT, ROLE_SYSTEM)
    }

    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (content.isBlank()) {
            errors.add("Content cannot be empty")
        }
        if (content.length > MAX_CONTENT_LENGTH) {
            errors.add("Content too long (max $MAX_CONTENT_LENGTH characters)")
        }
        if (role !in listOf(ROLE_USER, ROLE_ASSISTANT, ROLE_SYSTEM)) {
            errors.add("Invalid role: $role")
        }

        return errors
    }
}

/**
 * Attachment info data class
 */
data class AttachmentInfo(
    val type: String,  // "audio", "image", "document"
    val path: String,  // File path
    val name: String   // File name
)

/**
 * Message metadata data class
 */
data class MessageMetadata(
    val tokens: Int = 0,
    val model: String = "",
    val processingTime: Long = 0
) {
    fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("tokens", tokens)
        jsonObject.put("model", model)
        jsonObject.put("processingTime", processingTime)
        return jsonObject.toString()
    }
}
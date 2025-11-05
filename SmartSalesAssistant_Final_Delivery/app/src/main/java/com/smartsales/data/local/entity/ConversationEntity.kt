package com.smartsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * Conversation Entity - Represents a chat conversation
 *
 * Features:
 * - Auto-generated ID
 * - Timestamps for creation and updates
 * - Analysis tracking
 * - Summary support
 */
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,              // Conversation title
    val summary: String? = null,    // AI-generated summary
    val createdAt: Long,             // Creation timestamp (millis)
    val updatedAt: Long,             // Last update timestamp (millis)
    val isAnalyzed: Boolean = false  // Whether customer analysis is complete
) {

    companion object {
        // Default values
        const val DEFAULT_TITLE = "New Conversation"
        const val MAX_TITLE_LENGTH = 200
        const val MAX_SUMMARY_LENGTH = 1000

        /**
         * Create new conversation with current timestamp
         */
        fun create(
            title: String = DEFAULT_TITLE,
            summary: String? = null
        ): ConversationEntity {
            val now = System.currentTimeMillis()
            return ConversationEntity(
                title = title.take(MAX_TITLE_LENGTH),
                summary = summary?.take(MAX_SUMMARY_LENGTH),
                createdAt = now,
                updatedAt = now,
                isAnalyzed = false
            )
        }

        /**
         * Get timestamp for "today" start (for filtering)
         */
        fun getTodayStartTimestamp(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        /**
         * Get timestamp for N days ago
         */
        fun getDaysAgoTimestamp(days: Int): Long {
            return System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        }
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Check if conversation was created today
     */
    val isCreatedToday: Boolean
        get() = createdAt >= getTodayStartTimestamp()

    /**
     * Check if conversation was updated today
     */
    val isUpdatedToday: Boolean
        get() = updatedAt >= getTodayStartTimestamp()

    /**
     * Get age in days
     */
    val ageInDays: Int
        get() = ((System.currentTimeMillis() - createdAt) / (24 * 60 * 60 * 1000)).toInt()

    /**
     * Check if conversation is new (less than 1 hour old)
     */
    val isNew: Boolean
        get() = System.currentTimeMillis() - createdAt < 60 * 60 * 1000

    /**
     * Check if conversation is stale (not updated in 7+ days)
     */
    val isStale: Boolean
        get() = System.currentTimeMillis() - updatedAt > 7 * 24 * 60 * 60 * 1000

    /**
     * Check if conversation has summary
     */
    val hasSummary: Boolean
        get() = !summary.isNullOrBlank()

    // ===== FORMATTING METHODS =====

    /**
     * Format creation date
     */
    fun getFormattedCreatedDate(pattern: String = "yyyy-MM-dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(createdAt))
    }

    /**
     * Format updated date
     */
    fun getFormattedUpdatedDate(pattern: String = "yyyy-MM-dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(updatedAt))
    }

    /**
     * Get relative time string (e.g., "2 hours ago", "Yesterday")
     */
    fun getRelativeTimeString(): String {
        val now = System.currentTimeMillis()
        val diff = now - updatedAt

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} minutes ago"
            diff < 2 * 60 * 60 * 1000 -> "1 hour ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hours ago"
            diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)} days ago"
            else -> getFormattedUpdatedDate("MMM dd, yyyy")
        }
    }

    // ===== VALIDATION METHODS =====

    /**
     * Validate conversation data
     */
    fun isValid(): Boolean {
        return title.isNotBlank() &&
                title.length <= MAX_TITLE_LENGTH &&
                (summary == null || summary.length <= MAX_SUMMARY_LENGTH)
    }

    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (title.isBlank()) {
            errors.add("Title cannot be empty")
        }
        if (title.length > MAX_TITLE_LENGTH) {
            errors.add("Title too long (max $MAX_TITLE_LENGTH characters)")
        }
        if (summary != null && summary.length > MAX_SUMMARY_LENGTH) {
            errors.add("Summary too long (max $MAX_SUMMARY_LENGTH characters)")
        }

        return errors
    }

    // ===== HELPER METHODS =====

    /**
     * Update with new summary
     */
    fun withSummary(newSummary: String): ConversationEntity {
        return copy(
            summary = newSummary.take(MAX_SUMMARY_LENGTH),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Mark as analyzed
     */
    fun markAnalyzed(): ConversationEntity {
        return copy(
            isAnalyzed = true,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Update title
     */
    fun withTitle(newTitle: String): ConversationEntity {
        return copy(
            title = newTitle.take(MAX_TITLE_LENGTH),
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Touch (update timestamp)
     */
    fun touch(): ConversationEntity {
        return copy(updatedAt = System.currentTimeMillis())
    }
}
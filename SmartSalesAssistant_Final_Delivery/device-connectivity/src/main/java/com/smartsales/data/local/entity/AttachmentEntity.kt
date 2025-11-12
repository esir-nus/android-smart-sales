package com.smartsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Attachment Entity - Represents file attachments
 *
 * Features:
 * - Multiple file types support
 * - File metadata tracking
 * - Transcription for audio files
 */
@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val type: String, // "audio" | "image" | "document"
    val filePath: String, // Local file path
    val fileName: String, // Original file name
    val fileSize: Long, // File size in bytes
    val mimeType: String, // MIME type
    val uploadedAt: Long, // Upload timestamp (millis)
    val transcription: String? = null, // Audio transcription (audio files only)
) {
    companion object {
        // Type constants
        const val TYPE_AUDIO = "audio"
        const val TYPE_IMAGE = "image"
        const val TYPE_DOCUMENT = "document"
        const val TYPE_VIDEO = "video"

        // Size limits
        const val MAX_FILE_SIZE = 100 * 1024 * 1024L // 100MB
        const val MAX_AUDIO_SIZE = 50 * 1024 * 1024L // 50MB
        const val MAX_IMAGE_SIZE = 10 * 1024 * 1024L // 10MB

        // Common MIME types
        const val MIME_AUDIO_MP3 = "audio/mpeg"
        const val MIME_AUDIO_WAV = "audio/wav"
        const val MIME_AUDIO_M4A = "audio/mp4"
        const val MIME_IMAGE_JPEG = "image/jpeg"
        const val MIME_IMAGE_PNG = "image/png"
        const val MIME_PDF = "application/pdf"

        /**
         * Create audio attachment
         */
        fun createAudio(
            messageId: Long,
            filePath: String,
            fileName: String,
            fileSize: Long,
            mimeType: String = MIME_AUDIO_MP3,
        ): AttachmentEntity {
            return AttachmentEntity(
                messageId = messageId,
                type = TYPE_AUDIO,
                filePath = filePath,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                uploadedAt = System.currentTimeMillis(),
                transcription = null,
            )
        }

        /**
         * Create image attachment
         */
        fun createImage(
            messageId: Long,
            filePath: String,
            fileName: String,
            fileSize: Long,
            mimeType: String = MIME_IMAGE_JPEG,
        ): AttachmentEntity {
            return AttachmentEntity(
                messageId = messageId,
                type = TYPE_IMAGE,
                filePath = filePath,
                fileName = fileName,
                fileSize = fileSize,
                mimeType = mimeType,
                uploadedAt = System.currentTimeMillis(),
                transcription = null,
            )
        }

        /**
         * Detect type from MIME type
         */
        fun getTypeFromMimeType(mimeType: String): String {
            return when {
                mimeType.startsWith("audio/") -> TYPE_AUDIO
                mimeType.startsWith("image/") -> TYPE_IMAGE
                mimeType.startsWith("video/") -> TYPE_VIDEO
                else -> TYPE_DOCUMENT
            }
        }

        /**
         * Format file size (human readable)
         */
        fun formatFileSize(bytes: Long): String {
            return when {
                bytes < 1024 -> "$bytes B"
                bytes < 1024 * 1024 -> "${bytes / 1024} KB"
                bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
                else -> "${bytes / (1024 * 1024 * 1024)} GB"
            }
        }
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Check if attachment is audio
     */
    val isAudio: Boolean
        get() = type == TYPE_AUDIO

    /**
     * Check if attachment is image
     */
    val isImage: Boolean
        get() = type == TYPE_IMAGE

    /**
     * Check if attachment is document
     */
    val isDocument: Boolean
        get() = type == TYPE_DOCUMENT

    /**
     * Check if attachment is video
     */
    val isVideo: Boolean
        get() = type == TYPE_VIDEO

    /**
     * Check if audio has transcription
     */
    val hasTranscription: Boolean
        get() = isAudio && !transcription.isNullOrBlank()

    /**
     * Check if file exists
     */
    val fileExists: Boolean
        get() = File(filePath).exists()

    /**
     * Get file extension
     */
    val fileExtension: String
        get() = fileName.substringAfterLast('.', "")

    /**
     * Get file name without extension
     */
    val fileNameWithoutExtension: String
        get() = fileName.substringBeforeLast('.')

    /**
     * Get formatted file size
     */
    val formattedFileSize: String
        get() = formatFileSize(fileSize)

    /**
     * Get file age in days
     */
    val ageInDays: Int
        get() = ((System.currentTimeMillis() - uploadedAt) / (24 * 60 * 60 * 1000)).toInt()

    /**
     * Check if file is large (>10MB)
     */
    val isLargeFile: Boolean
        get() = fileSize > 10 * 1024 * 1024

    // ===== FILE OPERATIONS =====

    /**
     * Get File object
     */
    fun getFile(): File {
        return File(filePath)
    }

    /**
     * Check if file size is within limits
     */
    fun isWithinSizeLimit(): Boolean {
        return when (type) {
            TYPE_AUDIO -> fileSize <= MAX_AUDIO_SIZE
            TYPE_IMAGE -> fileSize <= MAX_IMAGE_SIZE
            else -> fileSize <= MAX_FILE_SIZE
        }
    }

    /**
     * Get file's parent directory
     */
    fun getParentDirectory(): String {
        return File(filePath).parent ?: ""
    }

    /**
     * Delete file from disk
     */
    fun deleteFile(): Boolean {
        return try {
            getFile().delete()
        } catch (e: Exception) {
            false
        }
    }

    // ===== FORMATTING METHODS =====

    /**
     * Format upload date
     */
    fun getFormattedUploadDate(pattern: String = "yyyy-MM-dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(uploadedAt))
    }

    /**
     * Get relative upload time
     */
    fun getRelativeUploadTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - uploadedAt

        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
            else -> getFormattedUploadDate("MMM dd")
        }
    }

    /**
     * Get display name (truncated if too long)
     */
    fun getDisplayName(maxLength: Int = 30): String {
        return if (fileName.length <= maxLength) {
            fileName
        } else {
            val ext = fileExtension
            val nameWithoutExt = fileNameWithoutExtension
            val truncated = nameWithoutExt.take(maxLength - ext.length - 4)
            "$truncated...$ext"
        }
    }

    // ===== VALIDATION METHODS =====

    /**
     * Validate attachment data
     */
    fun isValid(): Boolean {
        return fileName.isNotBlank() &&
            filePath.isNotBlank() &&
            fileSize > 0 &&
            mimeType.isNotBlank() &&
            type in listOf(TYPE_AUDIO, TYPE_IMAGE, TYPE_DOCUMENT, TYPE_VIDEO)
    }

    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (fileName.isBlank()) {
            errors.add("File name cannot be empty")
        }
        if (filePath.isBlank()) {
            errors.add("File path cannot be empty")
        }
        if (fileSize <= 0) {
            errors.add("Invalid file size")
        }
        if (!isWithinSizeLimit()) {
            errors.add("File size exceeds limit for type: $type")
        }
        if (type !in listOf(TYPE_AUDIO, TYPE_IMAGE, TYPE_DOCUMENT, TYPE_VIDEO)) {
            errors.add("Invalid file type: $type")
        }
        if (!fileExists) {
            errors.add("File does not exist at path: $filePath")
        }

        return errors
    }

    // ===== HELPER METHODS =====

    /**
     * Add transcription to audio attachment
     */
    fun withTranscription(text: String): AttachmentEntity {
        return copy(transcription = text)
    }

    /**
     * Get audio duration (if available in transcription metadata)
     * This is a placeholder - actual duration would need separate storage
     */
    fun getAudioDuration(): Int? {
        // TODO: Implement audio duration extraction
        return null
    }
}

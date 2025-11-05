package com.smartsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * CRM Export Entity - Tracks export history
 * 
 * Features:
 * - Export type tracking (CSV/PDF)
 * - File path storage
 * - Status tracking (success/failed)
 * - Export timestamp
 */
@Entity(tableName = "crm_exports")
data class CrmExportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val conversationId: Long,        // Source conversation ID
    val exportType: String,          // "csv" | "pdf"
    val filePath: String,            // File path (or error message if failed)
    val exportedAt: Long,            // Export timestamp (millis)
    val status: String               // "success" | "failed"
) {
    
    companion object {
        // Export type constants
        const val TYPE_CSV = "csv"
        const val TYPE_PDF = "pdf"
        const val TYPE_SALESFORCE = "salesforce_csv"
        const val TYPE_HUBSPOT = "hubspot_csv"
        
        // Status constants
        const val STATUS_SUCCESS = "success"
        const val STATUS_FAILED = "failed"
        const val STATUS_PENDING = "pending"
        const val STATUS_CANCELLED = "cancelled"
        
        // File size limits
        const val MAX_FILE_SIZE = 50 * 1024 * 1024L // 50MB
        
        // Time constants
        const val EXPORT_EXPIRY_DAYS = 30 // Auto-delete after 30 days
        
        /**
         * Create successful export record
         */
        fun createSuccess(
            conversationId: Long,
            exportType: String,
            filePath: String
        ): CrmExportEntity {
            return CrmExportEntity(
                conversationId = conversationId,
                exportType = exportType,
                filePath = filePath,
                exportedAt = System.currentTimeMillis(),
                status = STATUS_SUCCESS
            )
        }
        
        /**
         * Create failed export record
         */
        fun createFailed(
            conversationId: Long,
            exportType: String,
            errorMessage: String
        ): CrmExportEntity {
            return CrmExportEntity(
                conversationId = conversationId,
                exportType = exportType,
                filePath = errorMessage, // Store error in filePath
                exportedAt = System.currentTimeMillis(),
                status = STATUS_FAILED
            )
        }
        
        /**
         * Create pending export record
         */
        fun createPending(
            conversationId: Long,
            exportType: String
        ): CrmExportEntity {
            return CrmExportEntity(
                conversationId = conversationId,
                exportType = exportType,
                filePath = "", // Empty until export completes
                exportedAt = System.currentTimeMillis(),
                status = STATUS_PENDING
            )
        }
        
        /**
         * Get file extension for export type
         */
        fun getFileExtension(exportType: String): String {
            return when (exportType) {
                TYPE_CSV, TYPE_SALESFORCE, TYPE_HUBSPOT -> ".csv"
                TYPE_PDF -> ".pdf"
                else -> ""
            }
        }
        
        /**
         * Get MIME type for export type
         */
        fun getMimeType(exportType: String): String {
            return when (exportType) {
                TYPE_CSV, TYPE_SALESFORCE, TYPE_HUBSPOT -> "text/csv"
                TYPE_PDF -> "application/pdf"
                else -> "application/octet-stream"
            }
        }
    }
    
    // ===== COMPUTED PROPERTIES =====
    
    /**
     * Check if export was successful
     */
    val isSuccess: Boolean
        get() = status == STATUS_SUCCESS
    
    /**
     * Check if export failed
     */
    val isFailed: Boolean
        get() = status == STATUS_FAILED
    
    /**
     * Check if export is pending
     */
    val isPending: Boolean
        get() = status == STATUS_PENDING
    
    /**
     * Check if export was cancelled
     */
    val isCancelled: Boolean
        get() = status == STATUS_CANCELLED
    
    /**
     * Check if export is CSV type
     */
    val isCsv: Boolean
        get() = exportType in listOf(TYPE_CSV, TYPE_SALESFORCE, TYPE_HUBSPOT)
    
    /**
     * Check if export is PDF type
     */
    val isPdf: Boolean
        get() = exportType == TYPE_PDF
    
    /**
     * Check if export is for specific CRM
     */
    val isSalesforceCsv: Boolean
        get() = exportType == TYPE_SALESFORCE
    
    val isHubSpotCsv: Boolean
        get() = exportType == TYPE_HUBSPOT
    
    /**
     * Get file if export was successful
     */
    val file: File?
        get() = if (isSuccess && filePath.isNotBlank()) {
            File(filePath)
        } else {
            null
        }
    
    /**
     * Check if file exists
     */
    val fileExists: Boolean
        get() = file?.exists() == true
    
    /**
     * Get file size
     */
    val fileSize: Long
        get() = file?.length() ?: 0L
    
    /**
     * Get formatted file size
     */
    val formattedFileSize: String
        get() = formatFileSize(fileSize)
    
    /**
     * Get export age in days
     */
    val ageInDays: Int
        get() = ((System.currentTimeMillis() - exportedAt) / (24 * 60 * 60 * 1000)).toInt()
    
    /**
     * Check if export is recent (less than 24 hours old)
     */
    val isRecent: Boolean
        get() = ageInDays < 1
    
    /**
     * Check if export is expired (older than expiry period)
     */
    val isExpired: Boolean
        get() = ageInDays > EXPORT_EXPIRY_DAYS
    
    /**
     * Check if export is large (>5MB)
     */
    val isLargeFile: Boolean
        get() = fileSize > 5 * 1024 * 1024
    
    /**
     * Get error message (if failed)
     */
    val errorMessage: String?
        get() = if (isFailed) filePath else null
    
    /**
     * Get file name from path
     */
    val fileName: String
        get() = file?.name ?: ""
    
    /**
     * Get file extension
     */
    val fileExtension: String
        get() = fileName.substringAfterLast('.', "")
    
    // ===== FORMATTING METHODS =====
    
    /**
     * Format export timestamp
     */
    fun getFormattedExportDate(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(exportedAt))
    }
    
    /**
     * Get relative export time
     */
    fun getRelativeExportTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - exportedAt
        
        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
            else -> getFormattedExportDate("MMM dd, yyyy")
        }
    }
    
    /**
     * Format file size
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "${bytes / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Get export type display name
     */
    fun getExportTypeDisplayName(): String {
        return when (exportType) {
            TYPE_CSV -> "Generic CSV"
            TYPE_PDF -> "PDF Report"
            TYPE_SALESFORCE -> "Salesforce CSV"
            TYPE_HUBSPOT -> "HubSpot CSV"
            else -> exportType
        }
    }
    
    /**
     * Get status display name
     */
    fun getStatusDisplayName(): String {
        return when (status) {
            STATUS_SUCCESS -> "Success"
            STATUS_FAILED -> "Failed"
            STATUS_PENDING -> "Pending"
            STATUS_CANCELLED -> "Cancelled"
            else -> status
        }
    }
    
    /**
     * Get status emoji
     */
    fun getStatusEmoji(): String {
        return when (status) {
            STATUS_SUCCESS -> "✅"
            STATUS_FAILED -> "❌"
            STATUS_PENDING -> "⏳"
            STATUS_CANCELLED -> "🚫"
            else -> "❓"
        }
    }
    
    /**
     * Get summary string
     */
    fun getSummary(): String {
        return if (isSuccess) {
            "${getExportTypeDisplayName()} • ${formattedFileSize} • ${getRelativeExportTime()}"
        } else {
            "${getExportTypeDisplayName()} • ${getStatusDisplayName()} • ${getRelativeExportTime()}"
        }
    }
    
    // ===== FILE OPERATIONS =====
    
    /**
     * Delete exported file
     */
    fun deleteFile(): Boolean {
        return try {
            file?.delete() ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if file can be shared
     */
    fun canShare(): Boolean {
        return isSuccess && fileExists && fileSize > 0
    }
    
    /**
     * Get file MIME type
     */
    fun getFileMimeType(): String {
        return getMimeType(exportType)
    }
    
    /**
     * Get file URI (for sharing)
     */
    fun getFileUri(): String {
        return file?.absolutePath ?: ""
    }
    
    // ===== VALIDATION METHODS =====
    
    /**
     * Validate export record
     */
    fun isValid(): Boolean {
        return exportType.isNotBlank() &&
               status.isNotBlank() &&
               (isSuccess == fileExists || !isSuccess)
    }
    
    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        
        if (exportType.isBlank()) {
            errors.add("Export type cannot be empty")
        }
        if (status.isBlank()) {
            errors.add("Status cannot be empty")
        }
        if (isSuccess && !fileExists) {
            errors.add("Success status but file not found: $filePath")
        }
        if (isSuccess && fileSize == 0L) {
            errors.add("Success status but file is empty")
        }
        if (isSuccess && fileSize > MAX_FILE_SIZE) {
            errors.add("File size exceeds limit: ${formattedFileSize}")
        }
        
        return errors
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Mark as success
     */
    fun markSuccess(newFilePath: String): CrmExportEntity {
        return copy(
            filePath = newFilePath,
            status = STATUS_SUCCESS
        )
    }
    
    /**
     * Mark as failed
     */
    fun markFailed(error: String): CrmExportEntity {
        return copy(
            filePath = error,
            status = STATUS_FAILED
        )
    }
    
    /**
     * Mark as cancelled
     */
    fun markCancelled(): CrmExportEntity {
        return copy(status = STATUS_CANCELLED)
    }
    
    /**
     * Get shareable info
     */
    fun getShareInfo(): ShareInfo? {
        return if (canShare()) {
            ShareInfo(
                fileName = fileName,
                fileSize = fileSize,
                mimeType = getFileMimeType(),
                filePath = filePath
            )
        } else {
            null
        }
    }
}

/**
 * Share info data class
 */
data class ShareInfo(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val filePath: String
)
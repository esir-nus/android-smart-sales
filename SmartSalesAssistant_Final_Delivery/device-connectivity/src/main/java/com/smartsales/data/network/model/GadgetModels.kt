package com.smartsales.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Gadget HTTP API Models
 */

// ===== FILE MODELS =====

/**
 * File list response
 */
data class FileListResponse(
    @SerializedName("files")
    val files: List<GadgetFile>,
    
    @SerializedName("total_count")
    val totalCount: Int,
    
    @SerializedName("total_size")
    val totalSize: Long // Bytes
)

data class GadgetFile(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("path")
    val path: String,
    
    @SerializedName("size")
    val size: Long, // Bytes
    
    @SerializedName("type")
    val type: String, // "audio", "image", "document"
    
    @SerializedName("mime_type")
    val mimeType: String,
    
    @SerializedName("created_at")
    val createdAt: Long, // Timestamp
    
    @SerializedName("synced")
    val synced: Boolean = false
) {
    /**
     * Get formatted file size
     */
    fun getFormattedSize(): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }
    
    /**
     * Check if file is audio
     */
    fun isAudio(): Boolean = type == "audio"
    
    /**
     * Check if file is image
     */
    fun isImage(): Boolean = type == "image"
    
    /**
     * Get file extension
     */
    fun getExtension(): String {
        return name.substringAfterLast('.', "")
    }
}

// ===== DEVICE STATUS =====

/**
 * Device status response
 */
data class DeviceStatusResponse(
    @SerializedName("battery_level")
    val batteryLevel: Int, // 0-100
    
    @SerializedName("wifi_connected")
    val wifiConnected: Boolean,
    
    @SerializedName("wifi_ssid")
    val wifiSsid: String? = null,
    
    @SerializedName("storage_used")
    val storageUsed: Long, // Bytes
    
    @SerializedName("storage_total")
    val storageTotal: Long, // Bytes
    
    @SerializedName("recording")
    val recording: Boolean,
    
    @SerializedName("temperature")
    val temperature: Float? = null, // Celsius
    
    @SerializedName("uptime")
    val uptime: Long, // Seconds
    
    @SerializedName("firmware_version")
    val firmwareVersion: String
) {
    /**
     * Get storage usage percentage
     */
    fun getStorageUsagePercent(): Int {
        return if (storageTotal > 0) {
            ((storageUsed.toFloat() / storageTotal) * 100).toInt()
        } else 0
    }
    
    /**
     * Get formatted storage used
     */
    fun getFormattedStorageUsed(): String {
        return formatBytes(storageUsed)
    }
    
    /**
     * Get formatted storage total
     */
    fun getFormattedStorageTotal(): String {
        return formatBytes(storageTotal)
    }
    
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            else -> "%.2f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Get formatted uptime
     */
    fun getFormattedUptime(): String {
        val hours = uptime / 3600
        val minutes = (uptime % 3600) / 60
        return "${hours}h ${minutes}m"
    }
    
    /**
     * Check if storage is low (<10%)
     */
    fun isStorageLow(): Boolean {
        return getStorageUsagePercent() > 90
    }
    
    /**
     * Check if battery is low (<20%)
     */
    fun isBatteryLow(): Boolean {
        return batteryLevel < 20
    }
}

// ===== FILE DOWNLOAD REQUEST =====

data class FileDownloadRequest(
    @SerializedName("file_id")
    val fileId: String
)

// ===== FILE DELETE REQUEST =====

data class FileDeleteRequest(
    @SerializedName("file_ids")
    val fileIds: List<String>
)

data class FileDeleteResponse(
    @SerializedName("deleted_count")
    val deletedCount: Int,
    
    @SerializedName("failed_ids")
    val failedIds: List<String>? = null
)

// ===== WIFI STATUS =====

data class WifiStatusResponse(
    @SerializedName("connected")
    val connected: Boolean,
    
    @SerializedName("ssid")
    val ssid: String? = null,
    
    @SerializedName("signal_strength")
    val signalStrength: Int? = null, // -100 to 0 dBm
    
    @SerializedName("ip_address")
    val ipAddress: String? = null,
    
    @SerializedName("mac_address")
    val macAddress: String? = null
) {
    /**
     * Get signal strength percentage
     */
    fun getSignalStrengthPercent(): Int {
        return if (signalStrength != null) {
            // Convert dBm to percentage (approximate)
            val percent = 2 * (signalStrength + 100)
            percent.coerceIn(0, 100)
        } else 0
    }
    
    /**
     * Get signal quality description
     */
    fun getSignalQuality(): String {
        val percent = getSignalStrengthPercent()
        return when {
            percent >= 80 -> "Excellent"
            percent >= 60 -> "Good"
            percent >= 40 -> "Fair"
            percent >= 20 -> "Weak"
            else -> "Very Weak"
        }
    }
}

// ===== SYNC PROGRESS =====

data class SyncProgress(
    val fileName: String,
    val fileSize: Long,
    val downloadedBytes: Long,
    val progress: Int // 0-100
) {
    fun isComplete(): Boolean = progress >= 100
    
    fun getDownloadSpeed(): String {
        // This would be calculated based on time elapsed
        // Placeholder implementation
        return "0 KB/s"
    }
}
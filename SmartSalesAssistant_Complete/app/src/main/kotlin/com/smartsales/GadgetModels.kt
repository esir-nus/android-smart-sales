package com.smartsales.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Gadget API Models
 */
data class FileListResponse(
    val success: Boolean,
    val files: List<GadgetFile>
)

data class GadgetFile(
    val filename: String,
    val type: String,          // "audio" | "image"
    val size: Long,
    @SerializedName("created_at")
    val createdAt: Long,
    val path: String
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val filename: String?
)

data class TextDisplayRequest(
    val text: String,          // Max 10 characters
    val duration: Int? = null  // Display duration (seconds), null = persistent
)

data class DeviceStatusResponse(
    val success: Boolean,
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("battery_level")
    val batteryLevel: Int,     // 0-100
    @SerializedName("storage_used")
    val storageUsed: Long,     // bytes
    @SerializedName("storage_total")
    val storageTotal: Long,    // bytes
    @SerializedName("current_image")
    val currentImage: String?,
    @SerializedName("current_text")
    val currentText: String?,
    @SerializedName("wifi_connected")
    val wifiConnected: Boolean,
    @SerializedName("recording_count")
    val recordingCount: Int,
    @SerializedName("image_count")
    val imageCount: Int
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)

package com.smartsales.domain.model

import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.data.local.entity.MessageEntity

/**
 * Domain model combining conversation with its messages
 */
data class ConversationWithMessages(
    val conversation: ConversationEntity,
    val messages: List<MessageEntity>
)

/**
 * Attachment information
 */
data class Attachment(
    val type: String,      // "audio" | "image" | "document"
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String
)

/**
 * CRM data extracted from conversations
 */
data class CrmData(
    val customerName: String?,
    val company: String?,
    val position: String?,
    val phone: String?,
    val email: String?,
    val wechat: String?,
    val industry: String?,
    val companySize: String?,
    val location: String?,
    val productInterest: String?,
    val budgetRange: String?,
    val decisionTimeline: String?,
    val painPoints: List<String>?,
    val competitors: List<String>?,
    val decisionMakers: List<String>?,
    val currentStage: String?,
    val nextFollowUp: String?,
    val notes: String?
)

/**
 * Formatted transcription result
 */
data class FormattedTranscription(
    val fullText: String,
    val speakers: List<SpeakerInfo>,
    val summary: String?,
    val totalDuration: Long
)

/**
 * Speaker information from diarization
 */
data class SpeakerInfo(
    val id: String,
    val label: String,
    var totalDuration: Long,
    var sentenceCount: Int
)

/**
 * Audio analysis result combining transcription, analysis, and CRM data
 */
data class AudioAnalysisResult(
    val transcription: FormattedTranscription,
    val analysis: String,
    val crmData: CrmData?
)

/**
 * Sync result from file synchronization
 */
data class SyncResult(
    val filesDownloaded: Int,
    val audioFiles: Int,
    val imageFiles: Int,
    val deviceStatus: DeviceStatus?
)

/**
 * Device status information
 */
data class DeviceStatus(
    val deviceId: String,
    val batteryLevel: Int,
    val storageUsed: Long,
    val storageTotal: Long,
    val currentImage: String?,
    val currentText: String?,
    val wifiConnected: Boolean,
    val recordingCount: Int,
    val imageCount: Int
)

package com.smartsales.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Tingwu Transcription API Models
 */

// ===== REQUEST MODELS =====

/**
 * Create transcription task request
 */
data class TranscriptionRequest(
    @SerializedName("type")
    val type: String = "transcription", // "transcription" or "meeting"
    @SerializedName("parameters")
    val parameters: TranscriptionParameters,
)

data class TranscriptionParameters(
    @SerializedName("file_url")
    val fileUrl: String? = null, // URL to audio file
    @SerializedName("speaker_count")
    val speakerCount: Int? = null, // Number of speakers (for diarization)
    @SerializedName("diarization_enabled")
    val diarizationEnabled: Boolean = true, // Enable speaker diarization
    @SerializedName("translate_enabled")
    val translateEnabled: Boolean = false,
    @SerializedName("target_language")
    val targetLanguage: String? = null, // "en", "zh", etc.
    @SerializedName("format")
    val format: String = "json", // "json", "srt", "txt"
)

// ===== RESPONSE MODELS =====

/**
 * Task creation response
 */
data class TranscriptionTaskResponse(
    @SerializedName("task_id")
    val taskId: String,
    @SerializedName("status")
    val status: String, // "queued", "processing", "completed", "failed"
    @SerializedName("created_at")
    val createdAt: Long,
)

/**
 * Task status response
 */
data class TranscriptionStatusResponse(
    @SerializedName("task_id")
    val taskId: String,
    @SerializedName("status")
    val status: String,
    @SerializedName("progress")
    val progress: Int, // 0-100
    @SerializedName("created_at")
    val createdAt: Long,
    @SerializedName("completed_at")
    val completedAt: Long? = null,
    @SerializedName("error")
    val error: TranscriptionError? = null,
)

data class TranscriptionError(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
)

/**
 * Transcription result response
 */
data class TranscriptionResultResponse(
    @SerializedName("task_id")
    val taskId: String,
    @SerializedName("transcription")
    val transcription: TranscriptionData,
)

data class TranscriptionData(
    @SerializedName("text")
    val text: String, // Full transcription text
    @SerializedName("segments")
    val segments: List<TranscriptionSegment>,
    @SerializedName("speakers")
    val speakers: List<Speaker>? = null,
    @SerializedName("language")
    val language: String,
    @SerializedName("duration")
    val duration: Float, // Audio duration in seconds
)

data class TranscriptionSegment(
    @SerializedName("id")
    val id: Int,
    @SerializedName("start")
    val start: Float, // Start time in seconds
    @SerializedName("end")
    val end: Float, // End time in seconds
    @SerializedName("text")
    val text: String,
    @SerializedName("speaker")
    val speaker: String? = null, // Speaker ID (if diarization enabled)
    @SerializedName("confidence")
    val confidence: Float? = null, // Confidence score 0.0-1.0
) {
    /**
     * Get segment duration
     */
    fun getDuration(): Float = end - start

    /**
     * Format time range
     */
    fun getTimeRange(): String {
        return "${formatTime(start)} - ${formatTime(end)}"
    }

    private fun formatTime(seconds: Float): String {
        val minutes = (seconds / 60).toInt()
        val secs = (seconds % 60).toInt()
        return String.format("%02d:%02d", minutes, secs)
    }
}

data class Speaker(
    @SerializedName("id")
    val id: String,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("total_time")
    val totalTime: Float, // Total speaking time in seconds
)

// ===== HELPER EXTENSIONS =====

/**
 * Check if task is complete
 */
fun TranscriptionStatusResponse.isComplete(): Boolean {
    return status == "completed"
}

/**
 * Check if task failed
 */
fun TranscriptionStatusResponse.isFailed(): Boolean {
    return status == "failed"
}

/**
 * Check if task is still processing
 */
fun TranscriptionStatusResponse.isProcessing(): Boolean {
    return status in listOf("queued", "processing")
}

/**
 * Get segments by speaker
 */
fun TranscriptionData.getSegmentsBySpeaker(speakerId: String): List<TranscriptionSegment> {
    return segments.filter { it.speaker == speakerId }
}

/**
 * Get speaker statistics
 */
fun TranscriptionData.getSpeakerStats(): Map<String, SpeakerStats> {
    val stats = mutableMapOf<String, SpeakerStats>()

    segments.forEach { segment ->
        val speakerId = segment.speaker ?: "Unknown"
        val current =
            stats.getOrDefault(
                speakerId,
                SpeakerStats(speakerId, 0, 0f),
            )

        stats[speakerId] =
            current.copy(
                segmentCount = current.segmentCount + 1,
                totalTime = current.totalTime + segment.getDuration(),
            )
    }

    return stats
}

data class SpeakerStats(
    val speakerId: String,
    val segmentCount: Int,
    val totalTime: Float,
) {
    fun getTimePercentage(totalDuration: Float): Float {
        return if (totalDuration > 0) (totalTime / totalDuration) * 100 else 0f
    }
}

/**
 * Export transcription as SRT format
 */
fun TranscriptionData.toSrtFormat(): String {
    return segments.mapIndexed { index, segment ->
        val startTime = formatSrtTime(segment.start)
        val endTime = formatSrtTime(segment.end)
        val speaker = if (segment.speaker != null) "[${segment.speaker}] " else ""

        """
        ${index + 1}
        $startTime --> $endTime
        $speaker${segment.text}
        """.trimIndent()
    }.joinToString("\n\n")
}

private fun formatSrtTime(seconds: Float): String {
    val hours = (seconds / 3600).toInt()
    val minutes = ((seconds % 3600) / 60).toInt()
    val secs = (seconds % 60).toInt()
    val millis = ((seconds % 1) * 1000).toInt()

    return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis)
}

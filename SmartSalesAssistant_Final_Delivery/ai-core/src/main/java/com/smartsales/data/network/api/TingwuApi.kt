package com.smartsales.data.network.api

import com.smartsales.data.network.AiApiConfig
import com.smartsales.data.network.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Tingwu API Interface
 * 
 * Qwen Audio Transcription & Diarization Service
 * 
 * Official Documentation:
 * https://help.aliyun.com/document_detail/464238.html
 */
interface TingwuApi {
    
    /**
     * Create Transcription Task
     * 
     * Upload audio file and create transcription task
     * 
     * Example:
     * ```
     * val request = TranscriptionRequest(
     *     type = "transcription",
     *     parameters = TranscriptionParameters(
     *         fileUrl = "https://...",
     *         speakerCount = 2,
     *         diarizationEnabled = true
     *     )
     * )
     * val response = api.createTranscriptionTask(request)
     * ```
     */
    @POST(AiApiConfig.Tingwu.CREATE_TASK)
    @Headers("Content-Type: application/json")
    suspend fun createTranscriptionTask(
        @Body request: TranscriptionRequest
    ): Response<TranscriptionTaskResponse>
    
    /**
     * Create Transcription Task with File Upload
     * 
     * Upload audio file directly (multipart/form-data)
     */
    @Multipart
    @POST(AiApiConfig.Tingwu.CREATE_TASK)
    suspend fun createTranscriptionTaskWithFile(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("speaker_count") speakerCount: RequestBody? = null,
        @Part("diarization_enabled") diarizationEnabled: RequestBody? = null
    ): Response<TranscriptionTaskResponse>
    
    /**
     * Get Task Status
     * 
     * Check transcription task progress
     * 
     * Example:
     * ```
     * val response = api.getTaskStatus(taskId = "task_123456")
     * if (response.isComplete()) {
     *     // Task finished, fetch result
     * }
     * ```
     */
    @GET(AiApiConfig.Tingwu.GET_TASK)
    suspend fun getTaskStatus(
        @Path("taskId") taskId: String
    ): Response<TranscriptionStatusResponse>
    
    /**
     * Get Transcription Result
     * 
     * Retrieve completed transcription with speaker diarization
     * 
     * Example:
     * ```
     * val response = api.getTranscriptionResult(taskId = "task_123456")
     * val transcription = response.transcription
     * val segments = transcription.segments
     * ```
     */
    @GET(AiApiConfig.Tingwu.GET_RESULT)
    suspend fun getTranscriptionResult(
        @Path("taskId") taskId: String
    ): Response<TranscriptionResultResponse>
    
    /**
     * Get Transcription Result with Format
     * 
     * Get result in specific format (json, srt, txt)
     */
    @GET(AiApiConfig.Tingwu.GET_RESULT)
    suspend fun getTranscriptionResultWithFormat(
        @Path("taskId") taskId: String,
        @Query("format") format: String = "json" // "json", "srt", "txt"
    ): Response<TranscriptionResultResponse>
    
    /**
     * Cancel Task
     * 
     * Cancel a pending or processing task
     */
    @DELETE(AiApiConfig.Tingwu.GET_TASK)
    suspend fun cancelTask(
        @Path("taskId") taskId: String
    ): Response<Unit>
}

/**
 * Tingwu API Helper
 * 
 * Utility functions for working with Tingwu API
 */
object TingwuApiHelper {
    
    /**
     * Poll task status until complete
     * 
     * @param taskId Task ID
     * @param api Tingwu API instance
     * @param pollInterval Polling interval in milliseconds
     * @param timeout Maximum wait time in milliseconds
     * @return Final task status or null if timeout
     */
    suspend fun pollTaskStatus(
        taskId: String,
        api: TingwuApi,
        pollInterval: Long = 5000L,
        timeout: Long = 300000L // 5 minutes
    ): TranscriptionStatusResponse? {
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeout) {
            val response = api.getTaskStatus(taskId)
            
            if (response.isSuccessful) {
                val status = response.body()
                
                when {
                    status?.isComplete() == true -> return status
                    status?.isFailed() == true -> return status
                    else -> {
                        // Still processing, wait and retry
                        kotlinx.coroutines.delay(pollInterval)
                    }
                }
            } else {
                // API error
                return null
            }
        }
        
        // Timeout
        return null
    }
    
    /**
     * Validate audio file
     */
    fun validateAudioFile(
        filePath: String,
        maxSizeMB: Int = 100
    ): Result<Unit> {
        val file = java.io.File(filePath)
        
        if (!file.exists()) {
            return Result.failure(Exception("File does not exist: $filePath"))
        }
        
        val fileSizeMB = file.length() / (1024.0 * 1024.0)
        if (fileSizeMB > maxSizeMB) {
            return Result.failure(
                Exception("File size exceeds limit: ${fileSizeMB}MB > ${maxSizeMB}MB")
            )
        }
        
        // Check file extension
        val extension = file.extension.lowercase()
        val supportedFormats = listOf("mp3", "wav", "m4a", "flac", "aac", "ogg")
        
        if (extension !in supportedFormats) {
            return Result.failure(
                Exception("Unsupported audio format: $extension. Supported: ${supportedFormats.joinToString()}")
            )
        }
        
        return Result.success(Unit)
    }
    
    /**
     * Format transcription for display
     */
    fun formatTranscriptionForDisplay(
        data: TranscriptionData,
        includeSpeaker: Boolean = true,
        includeTimestamps: Boolean = false
    ): String {
        return data.segments.joinToString("\n\n") { segment ->
            buildString {
                if (includeTimestamps) {
                    append("[${segment.getTimeRange()}] ")
                }
                
                if (includeSpeaker && segment.speaker != null) {
                    append("${segment.speaker}: ")
                }
                
                append(segment.text)
            }
        }
    }
    
    /**
     * Extract key phrases from transcription
     */
    fun extractKeyPhrases(
        data: TranscriptionData,
        minLength: Int = 3
    ): List<String> {
        val text = data.text
        
        // Simple keyword extraction (in production, use NLP library)
        val words = text.split(Regex("\\s+"))
        
        return words
            .filter { it.length >= minLength }
            .filter { !it.matches(Regex("[的了是在有和与或等]")) } // Filter common words
            .groupBy { it }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
    
    /**
     * Calculate speaking time ratio
     */
    fun calculateSpeakingRatio(data: TranscriptionData): Map<String, Float> {
        val stats = data.getSpeakerStats()
        val totalTime = data.duration
        
        return stats.mapValues { (_, stats) ->
            stats.getTimePercentage(totalTime)
        }
    }
    
    /**
     * Create conversation summary from segments
     */
    fun createConversationSummary(
        data: TranscriptionData,
        maxSegments: Int = 5
    ): String {
        // Get longest segments as they often contain key information
        val keySegments = data.segments
            .sortedByDescending { it.text.length }
            .take(maxSegments)
            .sortedBy { it.start } // Re-sort by time
        
        return keySegments.joinToString("\n\n") { segment ->
            "${segment.speaker ?: "Unknown"}: ${segment.text}"
        }
    }
    
    /**
     * Detect conversation turn changes
     */
    fun detectTurnChanges(data: TranscriptionData): List<Int> {
        val turnChanges = mutableListOf<Int>()
        var lastSpeaker: String? = null
        
        data.segments.forEachIndexed { index, segment ->
            if (segment.speaker != lastSpeaker && lastSpeaker != null) {
                turnChanges.add(index)
            }
            lastSpeaker = segment.speaker
        }
        
        return turnChanges
    }
    
    /**
     * Get average segment confidence
     */
    fun getAverageConfidence(data: TranscriptionData): Float {
        val segmentsWithConfidence = data.segments.filter { it.confidence != null }
        
        return if (segmentsWithConfidence.isNotEmpty()) {
            segmentsWithConfidence.mapNotNull { it.confidence }.average().toFloat()
        } else {
            0f
        }
    }
}

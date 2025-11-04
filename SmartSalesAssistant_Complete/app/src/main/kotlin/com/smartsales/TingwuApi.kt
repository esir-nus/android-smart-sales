package com.smartsales.data.remote.api

import com.smartsales.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for Qwen Tingwu Transcription API
 */
interface TingwuApi {
    
    @POST("tasks")
    suspend fun createTask(
        @Body request: TranscriptionRequest
    ): Response<TaskResponse>
    
    @GET("tasks/{taskId}")
    suspend fun getTaskStatus(
        @Path("taskId") taskId: String
    ): Response<TaskStatusResponse>
    
    @GET("tasks/{taskId}/transcription")
    suspend fun getTranscription(
        @Path("taskId") taskId: String
    ): Response<TranscriptionResult>
}

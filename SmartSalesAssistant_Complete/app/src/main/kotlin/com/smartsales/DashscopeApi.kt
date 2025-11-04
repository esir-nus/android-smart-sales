package com.smartsales.data.remote.api

import com.smartsales.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for Qwen Dashscope Chat API
 */
interface DashscopeApi {
    
    @POST("services/aigc/text-generation/generation")
    suspend fun chat(
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

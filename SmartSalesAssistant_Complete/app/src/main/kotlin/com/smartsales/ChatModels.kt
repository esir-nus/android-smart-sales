package com.smartsales.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Chat Request Models for Qwen Dashscope
 */
data class ChatRequest(
    val model: String,
    val input: ChatInput,
    val parameters: ChatParameters? = null
)

data class ChatInput(
    val messages: List<ChatMessage>
)

data class ChatMessage(
    val role: String,      // "system" | "user" | "assistant"
    val content: String
)

data class ChatParameters(
    @SerializedName("result_format")
    val resultFormat: String = "message",  // "text" | "message"
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    val temperature: Float? = null,
    @SerializedName("top_p")
    val topP: Float? = null,
    @SerializedName("top_k")
    val topK: Int? = null,
    @SerializedName("enable_search")
    val enableSearch: Boolean = false,
    val stop: List<String>? = null
)

/**
 * Chat Response Models
 */
data class ChatResponse(
    val output: ChatOutput,
    val usage: Usage,
    @SerializedName("request_id")
    val requestId: String
)

data class ChatOutput(
    val text: String?,
    @SerializedName("finish_reason")
    val finishReason: String,
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    @SerializedName("finish_reason")
    val finishReason: String,
    val message: ChatMessage
)

data class Usage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

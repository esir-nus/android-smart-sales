package com.smartsales.data.network.model

import com.google.gson.annotations.SerializedName

/**
 * Dashscope Chat API Models
 */

// ===== REQUEST MODELS =====

/**
 * Chat completion request
 */
data class ChatRequest(
    @SerializedName("model")
    val model: String,
    @SerializedName("input")
    val input: ChatInput,
    @SerializedName("parameters")
    val parameters: ChatParameters? = null,
)

data class ChatInput(
    @SerializedName("messages")
    val messages: List<ChatMessage>,
)

data class ChatMessage(
    @SerializedName("role")
    val role: String, // "system", "user", "assistant"
    @SerializedName("content")
    val content: String,
) {
    companion object {
        fun system(content: String) = ChatMessage("system", content)

        fun user(content: String) = ChatMessage("user", content)

        fun assistant(content: String) = ChatMessage("assistant", content)
    }
}

data class ChatParameters(
    @SerializedName("result_format")
    val resultFormat: String = "message", // "message" or "text"
    @SerializedName("max_tokens")
    val maxTokens: Int? = null,
    @SerializedName("temperature")
    val temperature: Float? = null, // 0.0 - 2.0
    @SerializedName("top_p")
    val topP: Float? = null, // 0.0 - 1.0
    @SerializedName("top_k")
    val topK: Int? = null,
    @SerializedName("enable_search")
    val enableSearch: Boolean? = null,
    @SerializedName("incremental_output")
    val incrementalOutput: Boolean? = null, // For streaming
    @SerializedName("seed")
    val seed: Int? = null,
    @SerializedName("repetition_penalty")
    val repetitionPenalty: Float? = null,
) {
    companion object {
        fun default() =
            ChatParameters(
                resultFormat = "message",
                temperature = 0.7f,
                topP = 0.9f,
            )

        fun streaming() =
            ChatParameters(
                resultFormat = "message",
                incrementalOutput = true,
                temperature = 0.7f,
            )
    }
}

// ===== RESPONSE MODELS =====

/**
 * Chat completion response
 */
data class ChatResponse(
    @SerializedName("output")
    val output: ChatOutput,
    @SerializedName("usage")
    val usage: ChatUsage,
    @SerializedName("request_id")
    val requestId: String,
)

data class ChatOutput(
    @SerializedName("text")
    val text: String? = null, // For text format
    @SerializedName("finish_reason")
    val finishReason: String? = null, // "stop", "length", "null"
    @SerializedName("choices")
    val choices: List<ChatChoice>? = null, // For message format
)

data class ChatChoice(
    @SerializedName("finish_reason")
    val finishReason: String,
    @SerializedName("message")
    val message: ChatMessage,
)

data class ChatUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int,
) {
    fun getTotalCost(): String {
        // Approximate cost calculation (adjust based on actual pricing)
        val costPerToken = 0.000002 // $0.002 per 1K tokens
        val totalCost = totalTokens * costPerToken
        return String.format("$%.4f", totalCost)
    }
}

// ===== STREAMING RESPONSE =====

/**
 * Server-Sent Event for streaming
 */
data class ChatStreamEvent(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("event")
    val event: String? = null,
    @SerializedName("data")
    val data: String? = null,
) {
    fun isComplete(): Boolean {
        return event == "done" || data?.contains("\"finish_reason\":\"stop\"") == true
    }
}

// ===== ERROR RESPONSE =====

data class ChatErrorResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("request_id")
    val requestId: String?,
)

// ===== HELPER EXTENSIONS =====

/**
 * Get assistant's response text
 */
fun ChatResponse.getAssistantMessage(): String? {
    return output.choices?.firstOrNull()?.message?.content
        ?: output.text
}

/**
 * Check if response is complete
 */
fun ChatResponse.isComplete(): Boolean {
    return output.finishReason == "stop" ||
        output.choices?.firstOrNull()?.finishReason == "stop"
}

/**
 * Create a simple chat request
 */
fun createSimpleChatRequest(
    messages: List<ChatMessage>,
    model: String = "qwen-turbo",
): ChatRequest {
    return ChatRequest(
        model = model,
        input = ChatInput(messages),
        parameters = ChatParameters.default(),
    )
}

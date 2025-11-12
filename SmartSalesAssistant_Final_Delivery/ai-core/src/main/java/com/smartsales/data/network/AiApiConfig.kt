package com.smartsales.data.network

/**
 * API configuration for Dashscope (Qwen chat) and Tingwu services.
 */
object AiApiConfig {
    const val DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/"
    const val TINGWU_BASE_URL = "https://tingwu.cn/"

    const val DASHCOPE_CONNECT_TIMEOUT_MS = 30_000L
    const val DASHCOPE_READ_TIMEOUT_MS = 60_000L
    const val DASHCOPE_WRITE_TIMEOUT_MS = 60_000L
    const val DASHCOPE_STREAM_TIMEOUT_MS = 300_000L

    const val TINGWU_CONNECT_TIMEOUT_MS = 30_000L
    const val TINGWU_READ_TIMEOUT_MS = 60_000L
    const val TINGWU_WRITE_TIMEOUT_MS = 60_000L

    object Dashscope {
        const val CHAT_COMPLETION = "api/v1/services/aigc/text-generation/generation"
        const val CHAT_STREAM = "api/v1/services/aigc/text-generation/generation"
    }

    object Tingwu {
        const val CREATE_TASK = "api/v1/tasks"
        const val GET_TASK = "api/v1/tasks/{taskId}"
        const val GET_RESULT = "api/v1/tasks/{taskId}/transcription"
    }

    object Models {
        const val QWEN_TURBO = "qwen-turbo"
        const val QWEN_PLUS = "qwen-plus"
        const val QWEN_MAX = "qwen-max"
        const val QWEN_MAX_LONGCONTEXT = "qwen-max-longcontext"

        const val DEFAULT_CHAT_MODEL = QWEN_TURBO
    }

    const val MAX_TOKENS = 2000
    const val MAX_CONTEXT_LENGTH = 8_000
    const val MAX_FILE_SIZE_MB = 100

    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_X_DASHSCOPE_SSE = "X-DashScope-SSE"

    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_MULTIPART = "multipart/form-data"
    const val CONTENT_TYPE_SSE = "text/event-stream"

    fun buildBearerAuth(apiKey: String): String = "Bearer $apiKey"

    object OSS {
        // These values are populated from BuildConfig in the app modules
        lateinit var ENDPOINT: String
        lateinit var BUCKET: String
        lateinit var ACCESS_KEY_ID: String
        lateinit var ACCESS_KEY_SECRET: String
    }
}

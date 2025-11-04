package com.smartsales.data.network

/**
 * API Configuration
 * 
 * Centralized configuration for all network endpoints
 */
object ApiConfig {
    
    // ===== BASE URLS =====
    
    /**
     * Dashscope API (Qwen Chat)
     * Official endpoint: https://dashscope.aliyuncs.com/
     */
    const val DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/"
    
    /**
     * Tingwu API (Qwen Transcription)
     * Official endpoint: https://tingwu.cn/
     */
    const val TINGWU_BASE_URL = "https://tingwu.cn/"
    
    /**
     * Gadget HTTP Server
     * Device's local IP address (discovered via BLE)
     * Format: http://192.168.x.x:8080/
     */
    const val GADGET_DEFAULT_PORT = 8080
    
    // ===== API VERSIONS =====
    
    const val DASHSCOPE_API_VERSION = "v1"
    const val TINGWU_API_VERSION = "v1"
    
    // ===== ENDPOINTS =====
    
    object Dashscope {
        const val CHAT_COMPLETION = "api/v1/services/aigc/text-generation/generation"
        const val CHAT_STREAM = "api/v1/services/aigc/text-generation/generation"
    }
    
    object Tingwu {
        const val CREATE_TASK = "api/v1/tasks"
        const val GET_TASK = "api/v1/tasks/{taskId}"
        const val GET_RESULT = "api/v1/tasks/{taskId}/transcription"
    }
    
    object Gadget {
        const val FILE_LIST = "api/files"
        const val FILE_DOWNLOAD = "api/files/download"
        const val FILE_DELETE = "api/files/delete"
        const val DEVICE_STATUS = "api/status"
        const val WIFI_STATUS = "api/wifi/status"
    }
    
    // ===== MODELS =====
    
    object Models {
        // Qwen chat models
        const val QWEN_TURBO = "qwen-turbo"
        const val QWEN_PLUS = "qwen-plus"
        const val QWEN_MAX = "qwen-max"
        const val QWEN_MAX_LONGCONTEXT = "qwen-max-longcontext"
        
        // Default model
        const val DEFAULT_CHAT_MODEL = QWEN_TURBO
    }
    
    // ===== TIMEOUTS (milliseconds) =====
    
    const val CONNECT_TIMEOUT = 30_000L      // 30 seconds
    const val READ_TIMEOUT = 60_000L         // 60 seconds
    const val WRITE_TIMEOUT = 60_000L        // 60 seconds
    const val STREAM_READ_TIMEOUT = 300_000L // 5 minutes for streaming
    
    // ===== RETRY CONFIGURATION =====
    
    const val MAX_RETRY_ATTEMPTS = 3
    const val RETRY_DELAY_MS = 1000L
    
    // ===== REQUEST LIMITS =====
    
    const val MAX_TOKENS = 2000
    const val MAX_CONTEXT_LENGTH = 8000
    const val MAX_FILE_SIZE_MB = 100
    
    // ===== HEADER KEYS =====
    
    const val HEADER_AUTHORIZATION = "Authorization"
    const val HEADER_CONTENT_TYPE = "Content-Type"
    const val HEADER_X_DASHSCOPE_SSE = "X-DashScope-SSE"
    
    // ===== CONTENT TYPES =====
    
    const val CONTENT_TYPE_JSON = "application/json"
    const val CONTENT_TYPE_MULTIPART = "multipart/form-data"
    const val CONTENT_TYPE_SSE = "text/event-stream"
    
    /**
     * Get Gadget base URL from IP address
     */
    fun getGadgetBaseUrl(ipAddress: String, port: Int = GADGET_DEFAULT_PORT): String {
        return "http://$ipAddress:$port/"
    }
    
    /**
     * Build Dashscope authorization header
     */
    fun buildDashscopeAuth(apiKey: String): String {
        return "Bearer $apiKey"
    }
    
    /**
     * Build Tingwu authorization header
     */
    fun buildTingwuAuth(apiKey: String): String {
        return "Bearer $apiKey"
    }
}
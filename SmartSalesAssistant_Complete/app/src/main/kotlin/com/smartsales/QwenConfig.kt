package com.smartsales.data.remote.api

/**
 * Configuration for Qwen AI APIs
 */
object QwenConfig {
    // Base URLs
    const val DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/"
    const val TINGWU_BASE_URL = "https://tingwu.aliyuncs.com/api/v1/"
    
    // API Keys (loaded from BuildConfig)
    var dashscopeApiKey: String = ""
    var tingwuApiKey: String = ""
    
    // Model Configuration
    const val CHAT_MODEL = "qwen-max"  // qwen-turbo, qwen-plus, qwen-max
    const val MAX_TOKENS = 2000
    const val TEMPERATURE = 0.7f
    
    // Tingwu Configuration
    const val TINGWU_LANGUAGE = "zh"  // zh, en
    const val ENABLE_DIARIZATION = true
    const val ENABLE_TIMESTAMP = true
    
    // Gadget HTTP Server Configuration
    const val DEFAULT_GADGET_IP = "192.168.4.1"  // Default AP mode IP
    const val GADGET_PORT = 8080
}

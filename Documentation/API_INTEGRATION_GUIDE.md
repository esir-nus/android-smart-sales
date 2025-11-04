# Smart Sales Assistant - API Integration Guide

## ðŸ“¡ Overview

This guide covers integration with all external APIs used in the Smart Sales Assistant application.

---

## ðŸ”‘ API Keys & Authentication

### 1. Qwen Dashscope (Chat API)

**Obtaining API Key:**
1. Visit [Alibaba Cloud DashScope](https://dashscope.aliyuncs.com/)
2. Sign up or log in with Alibaba Cloud account
3. Navigate to API Management
4. Create new API key for DashScope
5. Copy the API key

**Configuration:**
```kotlin
// local.properties (DO NOT commit)
DASHSCOPE_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

// build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "DASHSCOPE_API_KEY", "\"${project.findProperty("DASHSCOPE_API_KEY")}\"")
    }
}

// QwenConfig.kt
object QwenConfig {
    val dashscopeApiKey: String = BuildConfig.DASHSCOPE_API_KEY
}
```

**Authentication Header:**
```kotlin
Authorization: Bearer ${QwenConfig.dashscopeApiKey}
Content-Type: application/json
```

---

### 2. Qwen Tingwu (Transcription API)

**Obtaining API Key:**
1. Visit [Alibaba Cloud Tingwu](https://tingwu.aliyuncs.com/)
2. Activate Tingwu service
3. Navigate to API credentials
4. Generate API key
5. Copy the key

**Configuration:**
```kotlin
// local.properties
TINGWU_API_KEY=sk-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

// build.gradle.kts
buildConfigField("String", "TINGWU_API_KEY", "\"${project.findProperty("TINGWU_API_KEY")}\"")

// QwenConfig.kt
object QwenConfig {
    val tingwuApiKey: String = BuildConfig.TINGWU_API_KEY
}
```

**Authentication Header:**
```kotlin
Authorization: Bearer ${QwenConfig.tingwuApiKey}
Content-Type: application/json
```

---

### 3. Alibaba Cloud OSS (Audio Storage)

Tingwu requires audio files to be uploaded to OSS first.

**Setup:**
```kotlin
// Dependencies
implementation("com.aliyun.oss:oss-android-sdk:2.9.13")

// OssConfig.kt
object OssConfig {
    const val ENDPOINT = "https://oss-cn-hangzhou.aliyuncs.com"
    const val BUCKET_NAME = "your-bucket-name"
    val accessKeyId: String = BuildConfig.OSS_ACCESS_KEY_ID
    val accessKeySecret: String = BuildConfig.OSS_ACCESS_KEY_SECRET
}

// OssUploader.kt
class OssUploader(context: Context) {
    private val oss: OSS by lazy {
        val credentialProvider = OSSPlainTextAKSKCredentialProvider(
            OssConfig.accessKeyId,
            OssConfig.accessKeySecret
        )
        OSSClient(context, OssConfig.ENDPOINT, credentialProvider)
    }
    
    suspend fun uploadAudio(file: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val objectKey = "audio/${System.currentTimeMillis()}_${file.name}"
                val request = PutObjectRequest(
                    OssConfig.BUCKET_NAME,
                    objectKey,
                    file.absolutePath
                )
                
                oss.putObject(request)
                
                val url = "oss://${OssConfig.BUCKET_NAME}/$objectKey"
                Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

---

## ðŸ¤– Qwen Dashscope API

### Base URL
```
https://dashscope.aliyuncs.com/api/v1/
```

### Available Models
- `qwen-turbo` - Fast, cost-effective
- `qwen-plus` - Balanced performance
- `qwen-max` - Best quality (recommended)
- `qwen-max-longcontext` - Extended context window

### Chat Completion Endpoint

**Endpoint:** `POST /services/aigc/text-generation/generation`

**Request:**
```json
{
  "model": "qwen-max",
  "input": {
    "messages": [
      {
        "role": "system",
        "content": "You are a helpful assistant."
      },
      {
        "role": "user",
        "content": "Hello, how are you?"
      }
    ]
  },
  "parameters": {
    "result_format": "message",
    "max_tokens": 2000,
    "temperature": 0.7,
    "top_p": 0.8,
    "enable_search": false
  }
}
```

**Response:**
```json
{
  "output": {
    "text": null,
    "finish_reason": "stop",
    "choices": [
      {
        "finish_reason": "stop",
        "message": {
          "role": "assistant",
          "content": "Hello! I'm doing well, thank you for asking. How can I assist you today?"
        }
      }
    ]
  },
  "usage": {
    "input_tokens": 25,
    "output_tokens": 18,
    "total_tokens": 43
  },
  "request_id": "abc123-def456-ghi789"
}
```

### Implementation Example

```kotlin
suspend fun chat(messages: List<ChatMessage>): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(
                model = "qwen-max",
                input = ChatInput(messages = messages),
                parameters = ChatParameters(
                    result_format = "message",
                    max_tokens = 2000,
                    temperature = 0.7f
                )
            )
            
            val response = dashscopeApi.chat(request)
            
            if (response.isSuccessful) {
                val body = response.body()!!
                val content = body.output.choices?.firstOrNull()?.message?.content
                    ?: return@withContext Result.failure(Exception("Empty response"))
                
                Log.d("API", "Tokens used: ${body.usage.total_tokens}")
                Result.success(content)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("API Error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Rate Limits
- **Free Tier:** 50 requests/minute, 1M tokens/month
- **Paid Tier:** Configurable, typically 200 requests/minute

### Error Codes
| Code | Meaning | Solution |
|------|---------|----------|
| 400 | Bad Request | Check request format |
| 401 | Unauthorized | Verify API key |
| 429 | Rate Limit | Implement exponential backoff |
| 500 | Server Error | Retry after delay |

---

## ðŸŽ¤ Qwen Tingwu API

### Base URL
```
https://tingwu.aliyuncs.com/api/v1/
```

### Workflow
1. Upload audio to OSS
2. Create transcription task
3. Poll task status
4. Retrieve results

### 1. Create Transcription Task

**Endpoint:** `POST /tasks`

**Request:**
```json
{
  "input": {
    "source_language": "zh",
    "task_key": "oss://bucket-name/audio/file.mp3",
    "format": "mp3"
  },
  "parameters": {
    "transcription": {
      "output_level": "sentence",
      "enable_punctuation": true,
      "enable_inverse_text_normalization": true
    },
    "diarization": {
      "enable": true,
      "speaker_count": null
    },
    "summarization": {
      "types": ["paragraph", "conversational"]
    }
  }
}
```

**Response:**
```json
{
  "task_id": "task_abc123def456",
  "status": "QUEUING",
  "request_id": "req_xyz789"
}
```

### 2. Check Task Status

**Endpoint:** `GET /tasks/{taskId}`

**Response:**
```json
{
  "task_id": "task_abc123def456",
  "status": "RUNNING",
  "progress": 45,
  "created_at": "2025-11-03T10:00:00Z",
  "updated_at": "2025-11-03T10:02:00Z",
  "request_id": "req_xyz789"
}
```

**Status Values:**
- `QUEUING` - Task queued
- `RUNNING` - Processing (poll every 5 seconds)
- `SUCCEEDED` - Complete, retrieve results
- `FAILED` - Failed, check error message

### 3. Get Transcription Results

**Endpoint:** `GET /tasks/{taskId}/transcription`

**Response:**
```json
{
  "task_id": "task_abc123def456",
  "status": "SUCCEEDED",
  "transcription": {
    "sentences": [
      {
        "text": "å¤§å®¶å¥½ï¼Œæˆ‘æ˜¯é”€å”®ç»ç†å¼ ä¸‰ã€‚",
        "begin_time": 0,
        "end_time": 2500,
        "speaker_id": "speaker_0",
        "words": [
          {
            "text": "å¤§å®¶å¥½",
            "begin_time": 0,
            "end_time": 800,
            "confidence": 0.98
          }
        ]
      }
    ]
  },
  "diarization": {
    "speakers": [
      {
        "speaker_id": "speaker_0",
        "duration": 45000,
        "segments": [
          {
            "begin_time": 0,
            "end_time": 2500,
            "text": "å¤§å®¶å¥½ï¼Œæˆ‘æ˜¯é”€å”®ç»ç†å¼ ä¸‰ã€‚"
          }
        ]
      }
    ]
  },
  "summarization": {
    "paragraph": "ä¼šè®®è®¨è®ºäº†æœ¬å­£åº¦é”€å”®ç›®æ ‡...",
    "conversational": "å¼ ä¸‰ä»‹ç»äº†å›¢é˜Ÿæƒ…å†µï¼ŒæŽå››æå‡ºäº†å¸‚åœºç­–ç•¥..."
  },
  "request_id": "req_xyz789"
}
```

### Implementation Example

```kotlin
suspend fun transcribeAudio(audioFile: File): Result<String> {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Upload to OSS
            val ossUrl = ossUploader.uploadAudio(audioFile).getOrThrow()
            
            // 2. Create task
            val taskRequest = TranscriptionRequest(
                input = TranscriptionInput(
                    source_language = "zh",
                    task_key = ossUrl,
                    format = "mp3"
                ),
                parameters = TranscriptionParameters(
                    transcription = TranscriptionConfig(),
                    diarization = DiarizationConfig(enable = true),
                    summarization = SummarizationConfig()
                )
            )
            
            val createResponse = tingwuApi.createTask(taskRequest)
            val taskId = createResponse.body()?.task_id
                ?: return@withContext Result.failure(Exception("No task ID"))
            
            // 3. Poll status
            var attempts = 0
            while (attempts < 60) { // Max 5 minutes
                delay(5000)
                
                val statusResponse = tingwuApi.getTaskStatus(taskId)
                val status = statusResponse.body()?.status
                
                when (status) {
                    "SUCCEEDED" -> break
                    "FAILED" -> return@withContext Result.failure(
                        Exception("Transcription failed")
                    )
                }
                
                attempts++
            }
            
            // 4. Get results
            val resultResponse = tingwuApi.getTranscription(taskId)
            if (resultResponse.isSuccessful) {
                Result.success(taskId)
            } else {
                Result.failure(Exception("Failed to get results"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### Rate Limits
- **Free Tier:** 2 hours audio/month
- **Paid Tier:** Pay-per-use, ~Â¥0.5/minute

### Supported Formats
- MP3, WAV, M4A, FLAC
- Max file size: 500MB
- Max duration: 4 hours

---

## ðŸ”Œ Hardware Gadget HTTP API

The gadget runs a simple HTTP server when connected to WiFi.

### Default Configuration
- **Default IP:** Auto-discovered or `192.168.4.1` (AP mode)
- **Port:** `8080`
- **Protocol:** HTTP (no HTTPS)

### 1. List Files

**Endpoint:** `GET /api/files/list`

**Response:**
```json
{
  "success": true,
  "files": [
    {
      "filename": "recording_20251103_100530.mp3",
      "type": "audio",
      "size": 2457600,
      "createdAt": 1730628330000,
      "path": "/storage/audio/recording_20251103_100530.mp3"
    },
    {
      "filename": "logo.gif",
      "type": "image",
      "size": 45120,
      "createdAt": 1730628200000,
      "path": "/storage/images/logo.gif"
    }
  ]
}
```

### 2. Download File

**Endpoint:** `GET /api/files/download?filename=recording_20251103_100530.mp3`

**Response:** Binary file stream

**Implementation:**
```kotlin
suspend fun downloadFile(filename: String): Result<File> {
    return withContext(Dispatchers.IO) {
        try {
            val response = gadgetApi.downloadFile(filename)
            
            if (response.isSuccessful) {
                val body = response.body()!!
                val targetFile = File(audioDir, filename)
                
                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Result.success(targetFile)
            } else {
                Result.failure(Exception("Download failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 3. Upload Image

**Endpoint:** `POST /api/display/image`

**Request:** Multipart form data
```
Content-Type: multipart/form-data; boundary=----boundary

------boundary
Content-Disposition: form-data; name="image"; filename="logo.png"
Content-Type: image/png

<binary data>
------boundary--
```

**Response:**
```json
{
  "success": true,
  "message": "Image uploaded successfully",
  "filename": "logo.png"
}
```

**Constraints:**
- Max size: 50KB
- Dimensions: 80x80 pixels
- Formats: PNG, GIF

### 4. Update Display Text

**Endpoint:** `POST /api/display/text`

**Request:**
```json
{
  "text": "Helloå®¢æˆ·",
  "duration": null
}
```

**Constraints:**
- Max length: 10 characters (Chinese counts as 1)
- Duration: null = permanent, number = seconds

**Response:**
```json
{
  "success": true,
  "message": "Text updated"
}
```

### 5. Get Device Status

**Endpoint:** `GET /api/device/status`

**Response:**
```json
{
  "success": true,
  "deviceId": "SSP1_ABC123",
  "batteryLevel": 85,
  "storageUsed": 15728640,
  "storageTotal": 536870912,
  "currentImage": "logo.gif",
  "currentText": "HiSales",
  "wifiConnected": true,
  "recordingCount": 12,
  "imageCount": 3
}
```

### 6. Delete File

**Endpoint:** `DELETE /api/files/delete?filename=recording_old.mp3`

**Response:**
```json
{
  "success": true,
  "message": "File deleted"
}
```

---

## ðŸ”§ Error Handling Best Practices

### 1. Exponential Backoff

```kotlin
suspend fun <T> retryWithBackoff(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> Result<T>
): Result<T> {
    var currentDelay = initialDelay
    
    repeat(maxAttempts - 1) { attempt ->
        val result = block()
        if (result.isSuccess) return result
        
        Log.w("RetryLogic", "Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
        delay(currentDelay)
        
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    
    return block() // Last attempt
}

// Usage
val result = retryWithBackoff {
    dashscopeApi.chat(request)
}
```

### 2. Network Timeout Configuration

```kotlin
private fun createOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor(maxRetries = 3))
        .build()
}

class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var attempt = 0
        var response: Response? = null
        var exception: IOException? = null
        
        while (attempt < maxRetries) {
            try {
                response = chain.proceed(chain.request())
                if (response.isSuccessful || response.code !in listOf(408, 429, 500, 502, 503, 504)) {
                    return response
                }
                response.close()
            } catch (e: IOException) {
                exception = e
                Log.w("RetryInterceptor", "Attempt ${attempt + 1} failed", e)
            }
            
            attempt++
            if (attempt < maxRetries) {
                Thread.sleep(1000L * attempt)
            }
        }
        
        throw exception ?: IOException("Request failed after $maxRetries attempts")
    }
}
```

### 3. API Response Validation

```kotlin
suspend fun <T> safeApiCall(
    call: suspend () -> Response<T>
): Result<T> {
    return try {
        val response = call()
        
        when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            }
            response.code() == 401 -> {
                Result.failure(AuthException("Unauthorized: Check API key"))
            }
            response.code() == 429 -> {
                Result.failure(RateLimitException("Rate limit exceeded"))
            }
            response.code() in 500..599 -> {
                Result.failure(ServerException("Server error: ${response.code()}"))
            }
            else -> {
                val errorBody = response.errorBody()?.string()
                Result.failure(ApiException("API Error: ${response.code()} - $errorBody"))
            }
        }
    } catch (e: IOException) {
        Result.failure(NetworkException("Network error: ${e.message}", e))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

// Custom exceptions
class AuthException(message: String) : Exception(message)
class RateLimitException(message: String) : Exception(message)
class ServerException(message: String) : Exception(message)
class ApiException(message: String) : Exception(message)
class NetworkException(message: String, cause: Throwable) : Exception(message, cause)
```

---

## ðŸ“Š API Usage Monitoring

### Token Usage Tracking

```kotlin
class ApiUsageTracker {
    private val _tokenUsage = MutableStateFlow<TokenUsage>(TokenUsage())
    val tokenUsage: StateFlow<TokenUsage> = _tokenUsage.asStateFlow()
    
    fun trackChatUsage(usage: Usage) {
        val current = _tokenUsage.value
        _tokenUsage.value = current.copy(
            chatInputTokens = current.chatInputTokens + usage.input_tokens,
            chatOutputTokens = current.chatOutputTokens + usage.output_tokens,
            chatRequests = current.chatRequests + 1
        )
    }
    
    fun trackTranscriptionUsage(durationSeconds: Long) {
        val current = _tokenUsage.value
        _tokenUsage.value = current.copy(
            transcriptionMinutes = current.transcriptionMinutes + (durationSeconds / 60),
            transcriptionRequests = current.transcriptionRequests + 1
        )
    }
    
    fun estimateCost(): Double {
        val usage = _tokenUsage.value
        
        // Dashscope: ~Â¥0.02 per 1K tokens
        val chatCost = (usage.chatInputTokens + usage.chatOutputTokens) * 0.02 / 1000
        
        // Tingwu: ~Â¥0.5 per minute
        val transcriptionCost = usage.transcriptionMinutes * 0.5
        
        return chatCost + transcriptionCost
    }
}

data class TokenUsage(
    val chatInputTokens: Int = 0,
    val chatOutputTokens: Int = 0,
    val chatRequests: Int = 0,
    val transcriptionMinutes: Long = 0,
    val transcriptionRequests: Int = 0
)
```

---

## ðŸ§ª Testing APIs

### Mock API Responses

```kotlin
class MockQwenApi : DashscopeApi {
    override suspend fun chat(request: ChatRequest): Response<ChatResponse> {
        delay(1000) // Simulate network delay
        
        val mockResponse = ChatResponse(
            output = ChatOutput(
                text = null,
                finish_reason = "stop",
                choices = listOf(
                    ChatChoice(
                        finish_reason = "stop",
                        message = ChatMessage(
                            role = "assistant",
                            content = "This is a mock response for testing."
                        )
                    )
                )
            ),
            usage = Usage(
                input_tokens = 20,
                output_tokens = 10,
                total_tokens = 30
            ),
            request_id = "mock_${System.currentTimeMillis()}"
        )
        
        return Response.success(mockResponse)
    }
}

// In your DI module
@Provides
@Singleton
fun provideDashscopeApi(@Named("isDebug") isDebug: Boolean): DashscopeApi {
    return if (isDebug && USE_MOCK_API) {
        MockQwenApi()
    } else {
        QwenApiClient.getDashscopeApi()
    }
}
```

---

## ðŸ”’ Security Checklist

- [ ] API keys stored in `local.properties` (gitignored)
- [ ] Never log API keys or tokens
- [ ] Use HTTPS for all API calls (except local gadget)
- [ ] Implement certificate pinning for production
- [ ] Rotate API keys periodically
- [ ] Monitor for unauthorized API usage
- [ ] Implement rate limiting on client side
- [ ] Sanitize user inputs before API calls
- [ ] Handle sensitive data in memory only
- [ ] Clear authentication tokens on logout

---

## ðŸ“š Additional Resources

### Qwen Documentation
- Dashscope: https://help.aliyun.com/document_detail/2400395.html
- Tingwu: https://help.aliyun.com/document_detail/465394.html

### Code Examples
- Official SDK: https://github.com/aliyun/alibabacloud-gateway
- Community Examples: Search "Qwen Android integration"

### Support
- Alibaba Cloud Support: https://workorder-intl.console.aliyun.com/
- Community Forum: https://developer.aliyun.com/ask/

---

**Last Updated:** November 2025  
**Version:** 1.0.0

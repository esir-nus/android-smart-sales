# Smart Sales Assistant - AI Integration Module

## 3.7 AIé›†æˆæ¨¡å—

### 3.7.1 Qwen APIé…ç½®

```kotlin
// APIå¯†é’¥é…ç½®
object QwenConfig {
    const val DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/api/v1/"
    const val TINGWU_BASE_URL = "https://tingwu.aliyuncs.com/api/v1/"
    
    // ä»ŽçŽ¯å¢ƒå˜é‡æˆ–å®‰å…¨å­˜å‚¨èŽ·å–
    var dashscopeApiKey: String = ""
    var tingwuApiKey: String = ""
    
    // æ¨¡åž‹é…ç½®
    const val CHAT_MODEL = "qwen-max"  // qwen-turbo, qwen-plus, qwen-max
    const val MAX_TOKENS = 2000
    const val TEMPERATURE = 0.7f
    
    // Tingwué…ç½®
    const val TINGWU_LANGUAGE = "zh"  // zh, en
    const val ENABLE_DIARIZATION = true
    const val ENABLE_TIMESTAMP = true
}
```

---

### 3.7.2 Dashscope API (èŠå¤©)

```kotlin
// Dashscope APIæŽ¥å£
interface DashscopeApi {
    @POST("services/aigc/text-generation/generation")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}

// è¯·æ±‚æ•°æ®æ¨¡åž‹
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
    val result_format: String = "message",  // "text" | "message"
    val max_tokens: Int? = null,
    val temperature: Float? = null,
    val top_p: Float? = null,
    val top_k: Int? = null,
    val enable_search: Boolean = false,
    val stop: List<String>? = null
)

// å“åº”æ•°æ®æ¨¡åž‹
data class ChatResponse(
    val output: ChatOutput,
    val usage: Usage,
    val request_id: String
)

data class ChatOutput(
    val text: String?,
    val finish_reason: String,
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    val finish_reason: String,
    val message: ChatMessage
)

data class Usage(
    val input_tokens: Int,
    val output_tokens: Int,
    val total_tokens: Int
)
```

---

### 3.7.3 Tingwu API (è½¬å†™)

```kotlin
// Tingwu APIæŽ¥å£
interface TingwuApi {
    // åˆ›å»ºè½¬å†™ä»»åŠ¡
    @POST("tasks")
    suspend fun createTask(@Body request: TranscriptionRequest): Response<TaskResponse>
    
    // æŸ¥è¯¢ä»»åŠ¡çŠ¶æ€
    @GET("tasks/{taskId}")
    suspend fun getTaskStatus(@Path("taskId") taskId: String): Response<TaskStatusResponse>
    
    // èŽ·å–è½¬å†™ç»“æžœ
    @GET("tasks/{taskId}/transcription")
    suspend fun getTranscription(@Path("taskId") taskId: String): Response<TranscriptionResult>
}

// è½¬å†™è¯·æ±‚
data class TranscriptionRequest(
    val input: TranscriptionInput,
    val parameters: TranscriptionParameters
)

data class TranscriptionInput(
    val source_language: String,    // "zh" | "en"
    val task_key: String,           // éŸ³é¢‘æ–‡ä»¶URLæˆ–OSSè·¯å¾„
    val format: String = "mp3"      // "mp3" | "wav" | "m4a"
)

data class TranscriptionParameters(
    val transcription: TranscriptionConfig,
    val translation: TranslationConfig? = null,
    val summarization: SummarizationConfig? = null,
    val diarization: DiarizationConfig? = null
)

data class TranscriptionConfig(
    val output_level: String = "sentence",  // "word" | "sentence"
    val enable_punctuation: Boolean = true,
    val enable_inverse_text_normalization: Boolean = true
)

data class TranslationConfig(
    val target_language: String = "en"
)

data class SummarizationConfig(
    val types: List<String> = listOf("paragraph", "conversational")
)

data class DiarizationConfig(
    val enable: Boolean = true,
    val speaker_count: Int? = null  // nullè¡¨ç¤ºè‡ªåŠ¨è¯†åˆ«
)

// ä»»åŠ¡å“åº”
data class TaskResponse(
    val task_id: String,
    val status: String,
    val request_id: String
)

data class TaskStatusResponse(
    val task_id: String,
    val status: String,  // "QUEUING" | "RUNNING" | "SUCCEEDED" | "FAILED"
    val progress: Int,   // 0-100
    val created_at: String,
    val updated_at: String,
    val request_id: String
)

// è½¬å†™ç»“æžœ
data class TranscriptionResult(
    val task_id: String,
    val status: String,
    val transcription: TranscriptionData,
    val diarization: DiarizationData?,
    val summarization: SummarizationData?,
    val request_id: String
)

data class TranscriptionData(
    val sentences: List<Sentence>
)

data class Sentence(
    val text: String,
    val begin_time: Long,      // æ¯«ç§’
    val end_time: Long,        // æ¯«ç§’
    val speaker_id: String?,   // è¯´è¯äººID
    val words: List<Word>?
)

data class Word(
    val text: String,
    val begin_time: Long,
    val end_time: Long,
    val confidence: Float      // 0.0-1.0
)

data class DiarizationData(
    val speakers: List<Speaker>
)

data class Speaker(
    val speaker_id: String,
    val duration: Long,        // æ€»è¯´è¯æ—¶é•¿ï¼ˆæ¯«ç§’ï¼‰
    val segments: List<Segment>
)

data class Segment(
    val begin_time: Long,
    val end_time: Long,
    val text: String
)

data class SummarizationData(
    val paragraph: String?,
    val conversational: String?
)
```

---

### 3.7.4 Retrofitå®¢æˆ·ç«¯é…ç½®

```kotlin
object QwenApiClient {
    
    // Dashscopeå®¢æˆ·ç«¯
    private val dashscopeRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(QwenConfig.DASHSCOPE_BASE_URL)
            .client(createDashscopeOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    // Tingwuå®¢æˆ·ç«¯
    private val tingwuRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(QwenConfig.TINGWU_BASE_URL)
            .client(createTingwuOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private fun createDashscopeOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${QwenConfig.dashscopeApiKey}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(LoggingInterceptor())
            .build()
    }
    
    private fun createTingwuOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${QwenConfig.tingwuApiKey}")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(LoggingInterceptor())
            .build()
    }
    
    fun getDashscopeApi(): DashscopeApi {
        return dashscopeRetrofit.create(DashscopeApi::class.java)
    }
    
    fun getTingwuApi(): TingwuApi {
        return tingwuRetrofit.create(TingwuApi::class.java)
    }
}
```

---

### 3.7.5 æç¤ºè¯æ¨¡æ¿ç®¡ç†å™¨

```kotlin
object PromptTemplateManager {
    
    // ç³»ç»Ÿæç¤ºè¯
    const val SYSTEM_PROMPT = """
ä½ æ˜¯HiXXXï¼Œä¸€ä½èµ„æ·±çš„é”€å”®é¡¾é—®AIåŠ©æ‰‹ã€‚
ä½ çš„ä»»åŠ¡æ˜¯å¸®åŠ©é”€å”®äººå‘˜åˆ†æžå®¢æˆ·éœ€æ±‚ï¼Œåˆ¶å®šä¸ªæ€§åŒ–çš„é”€å”®ç­–ç•¥ï¼Œæå‡é”€å”®è½¬åŒ–çŽ‡ã€‚

æ ¸å¿ƒèƒ½åŠ›ï¼š
1. æ·±å…¥ç†è§£å®¢æˆ·å¯¹è¯ï¼Œæ•æ‰æ˜¾æ€§å’Œéšæ€§éœ€æ±‚
2. ç²¾å‡†åˆ†æžå®¢æˆ·ç”»åƒï¼ˆæ€§æ ¼ã€å†³ç­–é£Žæ ¼ã€å…³æ³¨ç‚¹ï¼‰
3. æä¾›å¯æ‰§è¡Œçš„é”€å”®ç­–ç•¥å’Œè¯æœ¯å»ºè®®
4. è¯†åˆ«é”€å”®æœºä¼šå’Œæ½œåœ¨é£Žé™©

è¾“å‡ºè¦æ±‚ï¼š
- ä½¿ç”¨Markdownæ ¼å¼ï¼Œç»“æž„æ¸…æ™°
- å…³é”®ä¿¡æ¯ç”¨**åŠ ç²—**æ ‡æ³¨
- è¯­è¨€ç®€æ´ä¸“ä¸šï¼Œé¿å…å†—ä½™
- æä¾›å…·ä½“å¯è¡Œçš„è¡ŒåŠ¨å»ºè®®
"""
    
    // å®¢æˆ·åˆ†æžæç¤ºè¯
    fun generateCustomerAnalysisPrompt(
        transcript: String?,
        userNotes: String?,
        customerName: String? = null,
        conversationContext: String? = null
    ): String {
        val sections = mutableListOf<String>()
        
        // æ·»åŠ å¯¹è¯è®°å½•
        transcript?.let {
            sections.add("""
## ðŸ“ å¯¹è¯è®°å½•ï¼ˆå·²è½¬å†™ï¼‰
$it
""")
        }
        
        // æ·»åŠ é”€å”®äººå‘˜å¤‡æ³¨
        userNotes?.let {
            sections.add("""
## ðŸ’¡ é”€å”®äººå‘˜å¤‡æ³¨
$it
""")
        }
        
        // æ·»åŠ åŽ†å²ä¸Šä¸‹æ–‡
        conversationContext?.let {
            sections.add("""
## ðŸ“š åŽ†å²å¯¹è¯ä¸Šä¸‹æ–‡
$it
""")
        }
        
        val customerTitle = customerName ?: "å®¢æˆ·"
        
        return """
${sections.joinToString("\n\n")}

è¯·åŸºäºŽä»¥ä¸Šä¿¡æ¯ï¼Œç”Ÿæˆä¸€ä»½å®Œæ•´çš„å®¢æˆ·åˆ†æžå’Œé”€å”®è·Ÿè¿›æ–¹æ¡ˆï¼š

# ðŸŽ¯ å®¢æˆ·ã€Œ$customerTitleã€ä¸ªæ€§åŒ–é”€å”®è·Ÿè¿›æ–¹æ¡ˆ

## ä¸€ã€å®¢æˆ·ç”»åƒé€Ÿå†™
- **å§“å/ç§°å‘¼**: ï¼ˆå¦‚æžœæåŠï¼‰
- **æ€§æ ¼ç‰¹å¾**: ï¼ˆå¤–å‘/å†…å‘ã€ç†æ€§/æ„Ÿæ€§ã€æžœæ–­/çŠ¹è±«ç­‰ï¼‰
- **æ²Ÿé€šé£Žæ ¼**: ï¼ˆä¸“ä¸šä¸¥è°¨/è½»æ¾å¹½é»˜/ç®€æ´é«˜æ•ˆç­‰ï¼‰
- **æ ¸å¿ƒå…³æ³¨ç‚¹**: ï¼ˆä»·æ ¼/å“è´¨/æœåŠ¡/æŠ€æœ¯/å£ç¢‘ç­‰ï¼ŒæŒ‰ä¼˜å…ˆçº§æŽ’åºï¼‰
- **å†³ç­–é£Žæ ¼**: ï¼ˆç‹¬ç«‹å†³ç­–/éœ€è¦ä»–äººæ„è§/å›¢é˜Ÿå†³ç­–ç­‰ï¼‰
- **è´­ä¹°é˜»åŠ›**: ï¼ˆé¢„ç®—é™åˆ¶/æ—¶é—´åŽ‹åŠ›/ç«žå“å¯¹æ¯”/å†…éƒ¨æµç¨‹ç­‰ï¼‰

## äºŒã€éœ€æ±‚åˆ†æž
### 1. æ˜¾æ€§éœ€æ±‚
ï¼ˆå®¢æˆ·æ˜Žç¡®è¡¨è¾¾çš„éœ€æ±‚å’ŒæœŸæœ›ï¼‰

### 2. éšæ€§éœ€æ±‚
ï¼ˆä»Žå¯¹è¯ä¸­æŽ¨æ–­å‡ºçš„æ½œåœ¨éœ€æ±‚å’Œç—›ç‚¹ï¼‰

### 3. éœ€æ±‚ä¼˜å…ˆçº§
1. é¦–è¦éœ€æ±‚ï¼š
2. æ¬¡è¦éœ€æ±‚ï¼š
3. æ½œåœ¨éœ€æ±‚ï¼š

## ä¸‰ã€æ ¸å¿ƒé”€å”®ç­–ç•¥
### 1. æ²Ÿé€šç­–ç•¥
ï¼ˆå¦‚ä½•ä¸Žæ­¤å®¢æˆ·æœ€æœ‰æ•ˆåœ°æ²Ÿé€šï¼‰

### 2. äº§å“å®šä½
ï¼ˆå¼ºè°ƒå“ªäº›äº§å“ç‰¹ç‚¹å’Œä»·å€¼ç‚¹ï¼‰

### 3. ä»·å€¼ä¸»å¼ 
ï¼ˆé’ˆå¯¹å®¢æˆ·æ ¸å¿ƒå…³æ³¨ç‚¹çš„å·®å¼‚åŒ–ä»·å€¼ï¼‰

## å››ã€è¡ŒåŠ¨è®¡åˆ’
### è¿‘æœŸè¡ŒåŠ¨ï¼ˆ1-3å¤©ï¼‰
- [ ] è¡ŒåŠ¨1ï¼šå…·ä½“å†…å®¹
- [ ] è¡ŒåŠ¨2ï¼šå…·ä½“å†…å®¹

### ä¸­æœŸè¡ŒåŠ¨ï¼ˆ1å‘¨å†…ï¼‰
- [ ] è¡ŒåŠ¨1ï¼šå…·ä½“å†…å®¹
- [ ] è¡ŒåŠ¨2ï¼šå…·ä½“å†…å®¹

## äº”ã€è¯æœ¯å»ºè®®
### åœºæ™¯1ï¼šå†æ¬¡è”ç³»å¼€åœºç™½
ã€ŒæŽ¨èè¯æœ¯å†…å®¹ã€

### åœºæ™¯2ï¼šå¤„ç†ä»·æ ¼å¼‚è®®
ã€ŒæŽ¨èè¯æœ¯å†…å®¹ã€

### åœºæ™¯3ï¼šä¿ƒæˆæˆäº¤
ã€ŒæŽ¨èè¯æœ¯å†…å®¹ã€

## å…­ã€é£Žé™©é¢„è­¦
âš ï¸ éœ€è¦æ³¨æ„çš„æ½œåœ¨é£Žé™©å’Œåº”å¯¹ç­–ç•¥

## ä¸ƒã€æˆäº¤æ¦‚çŽ‡è¯„ä¼°
**å½“å‰æˆäº¤æ¦‚çŽ‡**: X%
**è¯„ä¼°ä¾æ®**:
- æ­£é¢å› ç´ ï¼š
- è´Ÿé¢å› ç´ ï¼š
- å»ºè®®å…³æ³¨ï¼š
"""
    }
    
    // ä¼šè®®æ€»ç»“æç¤ºè¯
    fun generateMeetingSummaryPrompt(transcript: String): String {
        return """
è¯·å¯¹ä»¥ä¸‹ä¼šè®®å¯¹è¯è¿›è¡Œå…¨é¢æ€»ç»“ï¼š

## å¯¹è¯å†…å®¹
$transcript

è¯·ç”Ÿæˆä»¥ä¸‹æ ¼å¼çš„ä¼šè®®æ€»ç»“ï¼š

# ðŸ“‹ ä¼šè®®æ€»ç»“

## ä¼šè®®æ¦‚è¦
ï¼ˆ3-5å¥è¯æ¦‚æ‹¬ä¼šè®®ä¸»è¦å†…å®¹ï¼‰

## å…³é”®è®¨è®ºç‚¹
1. è®¨è®ºç‚¹1ï¼šå…·ä½“å†…å®¹å’Œç»“è®º
2. è®¨è®ºç‚¹2ï¼šå…·ä½“å†…å®¹å’Œç»“è®º
3. è®¨è®ºç‚¹3ï¼šå…·ä½“å†…å®¹å’Œç»“è®º

## é‡è¦å†³ç­–
- å†³ç­–1ï¼šå†…å®¹
- å†³ç­–2ï¼šå†…å®¹

## å¾…åŠžäº‹é¡¹
- [ ] ä»»åŠ¡1ï¼šè´Ÿè´£äººã€æˆªæ­¢æ—¶é—´
- [ ] ä»»åŠ¡2ï¼šè´Ÿè´£äººã€æˆªæ­¢æ—¶é—´

## ä¸‹ä¸€æ­¥è¡ŒåŠ¨
ï¼ˆå…·ä½“çš„åŽç»­è·Ÿè¿›è®¡åˆ’ï¼‰
"""
    }
    
    // CRMæ•°æ®æå–æç¤ºè¯
    fun generateCrmExtractionPrompt(transcript: String, userNotes: String?): String {
        return """
ä»Žä»¥ä¸‹ä¿¡æ¯ä¸­æå–CRMç³»ç»Ÿæ‰€éœ€çš„ç»“æž„åŒ–æ•°æ®ï¼š

## å¯¹è¯è®°å½•
$transcript

${userNotes?.let { "## å¤‡æ³¨\n$it" } ?: ""}

è¯·ä»¥JSONæ ¼å¼è¾“å‡ºä»¥ä¸‹å­—æ®µï¼ˆå¦‚æžœä¿¡æ¯ä¸å­˜åœ¨ï¼Œä½¿ç”¨nullï¼‰ï¼š

```json
{
  "customer_name": "å®¢æˆ·å§“å",
  "company": "å…¬å¸åç§°",
  "position": "èŒä½",
  "phone": "ç”µè¯",
  "email": "é‚®ç®±",
  "wechat": "å¾®ä¿¡å·",
  "industry": "æ‰€å±žè¡Œä¸š",
  "company_size": "å…¬å¸è§„æ¨¡ï¼ˆäººæ•°ï¼‰",
  "location": "åœ°ç†ä½ç½®",
  "product_interest": "æ„Ÿå…´è¶£çš„äº§å“/æœåŠ¡",
  "budget_range": "é¢„ç®—èŒƒå›´",
  "decision_timeline": "å†³ç­–æ—¶é—´çº¿",
  "pain_points": ["ç—›ç‚¹1", "ç—›ç‚¹2"],
  "competitors": ["ç«žå“1", "ç«žå“2"],
  "decision_makers": ["å†³ç­–äºº1", "å†³ç­–äºº2"],
  "current_stage": "å½“å‰é”€å”®é˜¶æ®µï¼ˆåˆæ­¥æŽ¥è§¦/éœ€æ±‚ç¡®è®¤/æ–¹æ¡ˆæ¼”ç¤º/å•†åŠ¡è°ˆåˆ¤/æˆäº¤ï¼‰",
  "next_follow_up": "ä¸‹æ¬¡è·Ÿè¿›è®¡åˆ’",
  "notes": "å…¶ä»–é‡è¦å¤‡æ³¨"
}
```

åªè¾“å‡ºJSONï¼Œä¸è¦åŒ…å«å…¶ä»–æ–‡å­—è¯´æ˜Žã€‚
"""
    }
    
    // å¿«é€Ÿé—®ç­”æç¤ºè¯
    fun generateQuickQAPrompt(question: String, conversationContext: String? = null): String {
        return if (conversationContext != null) {
            """
åŸºäºŽä»¥ä¸‹å¯¹è¯ä¸Šä¸‹æ–‡å›žç­”é—®é¢˜ï¼š

## å¯¹è¯ä¸Šä¸‹æ–‡
$conversationContext

## é—®é¢˜
$question

è¯·ç®€æ´ä¸“ä¸šåœ°å›žç­”ï¼Œå¦‚æžœéœ€è¦è¡¥å……ä¿¡æ¯è¯·æ˜Žç¡®è¯´æ˜Žã€‚
"""
        } else {
            question
        }
    }
}
```

---

### 3.7.6 AIèŠå¤©ç®¡ç†å™¨

```kotlin
class AiChatManager(
    private val dashscopeApi: DashscopeApi,
    private val conversationRepository: ConversationRepository
) {
    
    // å‘é€èŠå¤©æ¶ˆæ¯
    suspend fun chat(
        conversationId: Long,
        userMessage: String,
        useContext: Boolean = true
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // æž„å»ºæ¶ˆæ¯åŽ†å²
                val messages = mutableListOf<ChatMessage>()
                
                // æ·»åŠ ç³»ç»Ÿæç¤ºè¯
                messages.add(
                    ChatMessage(
                        role = "system",
                        content = PromptTemplateManager.SYSTEM_PROMPT
                    )
                )
                
                // æ·»åŠ åŽ†å²å¯¹è¯ï¼ˆå¦‚æžœå¯ç”¨ä¸Šä¸‹æ–‡ï¼‰
                if (useContext) {
                    val conversation = conversationRepository.getConversationWithMessages(conversationId)
                    conversation?.messages?.forEach { msg ->
                        messages.add(
                            ChatMessage(
                                role = msg.role,
                                content = msg.content
                            )
                        )
                    }
                }
                
                // æ·»åŠ æ–°æ¶ˆæ¯
                messages.add(
                    ChatMessage(
                        role = "user",
                        content = userMessage
                    )
                )
                
                // è°ƒç”¨API
                val request = ChatRequest(
                    model = QwenConfig.CHAT_MODEL,
                    input = ChatInput(messages = messages),
                    parameters = ChatParameters(
                        max_tokens = QwenConfig.MAX_TOKENS,
                        temperature = QwenConfig.TEMPERATURE
                    )
                )
                
                val response = dashscopeApi.chat(request)
                
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val assistantMessage = body.output.choices?.firstOrNull()?.message?.content
                        ?: body.output.text
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    
                    // ä¿å­˜æ¶ˆæ¯åˆ°æ•°æ®åº“
                    conversationRepository.addMessage(
                        conversationId = conversationId,
                        role = "user",
                        content = userMessage
                    )
                    
                    conversationRepository.addMessage(
                        conversationId = conversationId,
                        role = "assistant",
                        content = assistantMessage
                    )
                    
                    Result.success(assistantMessage)
                } else {
                    Result.failure(Exception("API Error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // åˆ†æžå®¢æˆ·ï¼ˆåŸºäºŽè½¬å†™å†…å®¹ï¼‰
    suspend fun analyzeCustomer(
        conversationId: Long,
        transcript: String,
        userNotes: String? = null,
        customerName: String? = null
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // èŽ·å–åŽ†å²ä¸Šä¸‹æ–‡
                val conversation = conversationRepository.getConversationWithMessages(conversationId)
                val context = conversation?.messages?.takeLast(5)?.joinToString("\n\n") { msg ->
                    "${msg.role}: ${msg.content}"
                }
                
                // ç”Ÿæˆåˆ†æžæç¤ºè¯
                val prompt = PromptTemplateManager.generateCustomerAnalysisPrompt(
                    transcript = transcript,
                    userNotes = userNotes,
                    customerName = customerName,
                    conversationContext = context
                )
                
                // è°ƒç”¨AI
                chat(conversationId, prompt, useContext = false)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // ç”Ÿæˆä¼šè®®æ€»ç»“
    suspend fun generateMeetingSummary(
        conversationId: Long,
        transcript: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = PromptTemplateManager.generateMeetingSummaryPrompt(transcript)
                chat(conversationId, prompt, useContext = false)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // æå–CRMæ•°æ®
    suspend fun extractCrmData(
        transcript: String,
        userNotes: String? = null
    ): Result<CrmData> {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = PromptTemplateManager.generateCrmExtractionPrompt(transcript, userNotes)
                
                val request = ChatRequest(
                    model = QwenConfig.CHAT_MODEL,
                    input = ChatInput(
                        messages = listOf(
                            ChatMessage(role = "system", content = "ä½ æ˜¯ä¸€ä¸ªæ•°æ®æå–åŠ©æ‰‹ï¼Œåªè¾“å‡ºJSONæ ¼å¼æ•°æ®ã€‚"),
                            ChatMessage(role = "user", content = prompt)
                        )
                    ),
                    parameters = ChatParameters(
                        max_tokens = 1500,
                        temperature = 0.3f  // é™ä½Žæ¸©åº¦ä»¥èŽ·å¾—æ›´ç¡®å®šçš„è¾“å‡º
                    )
                )
                
                val response = dashscopeApi.chat(request)
                
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val jsonText = body.output.choices?.firstOrNull()?.message?.content
                        ?: body.output.text
                        ?: return@withContext Result.failure(Exception("Empty response"))
                    
                    // æ¸…ç†JSONï¼ˆç§»é™¤markdownä»£ç å—ï¼‰
                    val cleanJson = jsonText
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()
                    
                    // è§£æžJSON
                    val gson = Gson()
                    val crmData = gson.fromJson(cleanJson, CrmData::class.java)
                    
                    Result.success(crmData)
                } else {
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// CRMæ•°æ®æ¨¡åž‹
data class CrmData(
    val customer_name: String?,
    val company: String?,
    val position: String?,
    val phone: String?,
    val email: String?,
    val wechat: String?,
    val industry: String?,
    val company_size: String?,
    val location: String?,
    val product_interest: String?,
    val budget_range: String?,
    val decision_timeline: String?,
    val pain_points: List<String>?,
    val competitors: List<String>?,
    val decision_makers: List<String>?,
    val current_stage: String?,
    val next_follow_up: String?,
    val notes: String?
)
```

---

### 3.7.7 éŸ³é¢‘è½¬å†™ç®¡ç†å™¨

```kotlin
class AudioTranscriptionManager(
    private val tingwuApi: TingwuApi,
    private val context: Context
) {
    
    private val _transcriptionProgress = MutableStateFlow<TranscriptionProgress>(TranscriptionProgress.Idle)
    val transcriptionProgress: StateFlow<TranscriptionProgress> = _transcriptionProgress.asStateFlow()
    
    // ä¸Šä¼ éŸ³é¢‘å¹¶åˆ›å»ºè½¬å†™ä»»åŠ¡
    suspend fun transcribeAudio(
        audioFile: File,
        enableDiarization: Boolean = true,
        enableSummarization: Boolean = true
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                _transcriptionProgress.value = TranscriptionProgress.Uploading
                
                // 1. ä¸Šä¼ éŸ³é¢‘æ–‡ä»¶ï¼ˆå®žé™…åº”ç”¨ä¸­éœ€è¦å…ˆä¸Šä¼ åˆ°OSSï¼‰
                val audioUrl = uploadToOss(audioFile)
                if (audioUrl.isFailure) {
                    return@withContext Result.failure(audioUrl.exceptionOrNull()!!)
                }
                
                _transcriptionProgress.value = TranscriptionProgress.Creating
                
                // 2. åˆ›å»ºè½¬å†™ä»»åŠ¡
                val taskRequest = TranscriptionRequest(
                    input = TranscriptionInput(
                        source_language = "zh",
                        task_key = audioUrl.getOrThrow(),
                        format = audioFile.extension
                    ),
                    parameters = TranscriptionParameters(
                        transcription = TranscriptionConfig(
                            output_level = "sentence",
                            enable_punctuation = true,
                            enable_inverse_text_normalization = true
                        ),
                        diarization = if (enableDiarization) {
                            DiarizationConfig(enable = true)
                        } else null,
                        summarization = if (enableSummarization) {
                            SummarizationConfig(types = listOf("paragraph", "conversational"))
                        } else null
                    )
                )
                
                val createResponse = tingwuApi.createTask(taskRequest)
                if (!createResponse.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to create task: ${createResponse.code()}")
                    )
                }
                
                val taskId = createResponse.body()!!.task_id
                
                // 3. è½®è¯¢ä»»åŠ¡çŠ¶æ€
                _transcriptionProgress.value = TranscriptionProgress.Processing(0)
                
                val result = pollTaskStatus(taskId)
                if (result.isFailure) {
                    return@withContext result
                }
                
                // 4. èŽ·å–è½¬å†™ç»“æžœ
                _transcriptionProgress.value = TranscriptionProgress.Fetching
                
                val transcriptionResult = tingwuApi.getTranscription(taskId)
                if (!transcriptionResult.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to get transcription: ${transcriptionResult.code()}")
                    )
                }
                
                _transcriptionProgress.value = TranscriptionProgress.Completed
                
                Result.success(taskId)
            } catch (e: Exception) {
                _transcriptionProgress.value = TranscriptionProgress.Error(e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }
    
    // è½®è¯¢ä»»åŠ¡çŠ¶æ€
    private suspend fun pollTaskStatus(taskId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 60  // æœ€å¤šç­‰å¾…5åˆ†é’Ÿï¼ˆæ¯5ç§’ä¸€æ¬¡ï¼‰
            
            while (attempts < maxAttempts) {
                delay(5000)  // ç­‰å¾…5ç§’
                
                val statusResponse = tingwuApi.getTaskStatus(taskId)
                if (!statusResponse.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Failed to get status: ${statusResponse.code()}")
                    )
                }
                
                val status = statusResponse.body()!!
                
                when (status.status) {
                    "SUCCEEDED" -> {
                        return@withContext Result.success(Unit)
                    }
                    "FAILED" -> {
                        return@withContext Result.failure(Exception("Transcription failed"))
                    }
                    "RUNNING" -> {
                        _transcriptionProgress.value = TranscriptionProgress.Processing(status.progress)
                    }
                }
                
                attempts++
            }
            
            Result.failure(Exception("Transcription timeout"))
        }
    }
    
    // èŽ·å–è½¬å†™ç»“æžœ
    suspend fun getTranscriptionResult(taskId: String): Result<TranscriptionResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response = tingwuApi.getTranscription(taskId)
                
                if (response.isSuccessful) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to get result: ${response.code()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // æ ¼å¼åŒ–è½¬å†™ç»“æžœä¸ºå¯è¯»æ–‡æœ¬
    fun formatTranscription(result: TranscriptionResult): FormattedTranscription {
        val fullText = StringBuilder()
        val speakers = mutableMapOf<String, SpeakerInfo>()
        
        result.transcription.sentences.forEach { sentence ->
            val speakerId = sentence.speaker_id ?: "Unknown"
            val speakerLabel = "è¯´è¯äºº${speakerId}"
            
            // æ”¶é›†è¯´è¯äººä¿¡æ¯
            if (!speakers.containsKey(speakerId)) {
                speakers[speakerId] = SpeakerInfo(
                    id = speakerId,
                    label = speakerLabel,
                    totalDuration = 0L,
                    sentenceCount = 0
                )
            }
            
            speakers[speakerId]?.let {
                it.totalDuration += (sentence.end_time - sentence.begin_time)
                it.sentenceCount++
            }
            
            // æ ¼å¼åŒ–æ—¶é—´æˆ³
            val timestamp = formatTimestamp(sentence.begin_time)
            
            // æ·»åŠ åˆ°å…¨æ–‡
            fullText.append("[$timestamp] $speakerLabel: ${sentence.text}\n\n")
        }
        
        return FormattedTranscription(
            fullText = fullText.toString(),
            speakers = speakers.values.toList(),
            summary = result.summarization?.conversational,
            totalDuration = result.transcription.sentences.lastOrNull()?.end_time ?: 0L
        )
    }
    
    private fun formatTimestamp(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    // æ¨¡æ‹ŸOSSä¸Šä¼ ï¼ˆå®žé™…åº”ç”¨ä¸­éœ€è¦å®žçŽ°çœŸå®žçš„OSSä¸Šä¼ ï¼‰
    private suspend fun uploadToOss(file: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // è¿™é‡Œåº”è¯¥å®žçŽ°çœŸå®žçš„é˜¿é‡Œäº‘OSSä¸Šä¼ é€»è¾‘
                // æš‚æ—¶è¿”å›žæ¨¡æ‹ŸURL
                val url = "oss://bucket-name/audio/${file.name}"
                Result.success(url)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// è½¬å†™è¿›åº¦çŠ¶æ€
sealed class TranscriptionProgress {
    object Idle : TranscriptionProgress()
    object Uploading : TranscriptionProgress()
    object Creating : TranscriptionProgress()
    data class Processing(val progress: Int) : TranscriptionProgress()
    object Fetching : TranscriptionProgress()
    object Completed : TranscriptionProgress()
    data class Error(val message: String) : TranscriptionProgress()
}

// æ ¼å¼åŒ–çš„è½¬å†™ç»“æžœ
data class FormattedTranscription(
    val fullText: String,
    val speakers: List<SpeakerInfo>,
    val summary: String?,
    val totalDuration: Long
)

data class SpeakerInfo(
    val id: String,
    val label: String,
    var totalDuration: Long,
    var sentenceCount: Int
)
```

---

### 3.7.8 AIä»“åº“ï¼ˆæ•´åˆï¼‰

```kotlin
class AiRepository(
    private val chatManager: AiChatManager,
    private val transcriptionManager: AudioTranscriptionManager,
    private val conversationRepository: ConversationRepository
) {
    
    // å®Œæ•´çš„éŸ³é¢‘åˆ†æžæµç¨‹
    suspend fun processAudioWithAnalysis(
        conversationId: Long,
        audioFile: File,
        userNotes: String? = null,
        customerName: String? = null
    ): Result<AudioAnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. è½¬å†™éŸ³é¢‘
                val transcriptionResult = transcriptionManager.transcribeAudio(
                    audioFile = audioFile,
                    enableDiarization = true,
                    enableSummarization = true
                )
                
                if (transcriptionResult.isFailure) {
                    return@withContext Result.failure(transcriptionResult.exceptionOrNull()!!)
                }
                
                val taskId = transcriptionResult.getOrThrow()
                
                // 2. èŽ·å–è½¬å†™ç»“æžœ
                val resultData = transcriptionManager.getTranscriptionResult(taskId)
                if (resultData.isFailure) {
                    return@withContext Result.failure(resultData.exceptionOrNull()!!)
                }
                
                val transcription = transcriptionManager.formatTranscription(resultData.getOrThrow())
                
                // 3. AIåˆ†æžå®¢æˆ·
                val analysisResult = chatManager.analyzeCustomer(
                    conversationId = conversationId,
                    transcript = transcription.fullText,
                    userNotes = userNotes,
                    customerName = customerName
                )
                
                if (analysisResult.isFailure) {
                    return@withContext Result.failure(analysisResult.exceptionOrNull()!!)
                }
                
                // 4. æå–CRMæ•°æ®
                val crmDataResult = chatManager.extractCrmData(
                    transcript = transcription.fullText,
                    userNotes = userNotes
                )
                
                Result.success(
                    AudioAnalysisResult(
                        transcription = transcription,
                        analysis = analysisResult.getOrThrow(),
                        crmData = crmDataResult.getOrNull()
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // æµå¼èŠå¤©ï¼ˆä¿æŒä¸Šä¸‹æ–‡ï¼‰
    fun chatStream(conversationId: Long) = flow {
        // æµå¼å®žçŽ°ï¼ˆå¦‚æžœAPIæ”¯æŒSSEï¼‰
        // è¿™é‡Œæ˜¯ç®€åŒ–ç‰ˆæœ¬
    }
}

data class AudioAnalysisResult(
    val transcription: FormattedTranscription,
    val analysis: String,
    val crmData: CrmData?
)
```

---

### 3.7.9 ä½¿ç”¨ç¤ºä¾‹

```kotlin
class ChatViewModel(
    private val aiRepository: AiRepository,
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    
    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()
    
    private val _messages = MutableStateFlow<List<MessageUi>>(emptyList())
    val messages: StateFlow<List<MessageUi>> = _messages.asStateFlow()
    
    // å‘é€æ¶ˆæ¯
    fun sendMessage(conversationId: Long, message: String) {
        viewModelScope.launch {
            _chatState.value = ChatUiState.Loading
            
            val result = aiRepository.chatManager.chat(
                conversationId = conversationId,
                userMessage = message,
                useContext = true
            )
            
            when {
                result.isSuccess -> {
                    loadMessages(conversationId)
                    _chatState.value = ChatUiState.Success
                }
                else -> {
                    _chatState.value = ChatUiState.Error(
                        result.exceptionOrNull()?.message ?: "å‘é€å¤±è´¥"
                    )
                }
            }
        }
    }
    
    // å¤„ç†éŸ³é¢‘æ–‡ä»¶
    fun processAudio(conversationId: Long, audioFile: File, notes: String? = null) {
        viewModelScope.launch {
            _chatState.value = ChatUiState.Processing
            
            val result = aiRepository.processAudioWithAnalysis(
                conversationId = conversationId,
                audioFile = audioFile,
                userNotes = notes
            )
            
            when {
                result.isSuccess -> {
                    val data = result.getOrThrow()
                    _chatState.value = ChatUiState.AudioProcessed(data)
                }
                else -> {
                    _chatState.value = ChatUiState.Error(
                        result.exceptionOrNull()?.message ?: "å¤„ç†å¤±è´¥"
                    )
                }
            }
        }
    }
    
    private suspend fun loadMessages(conversationId: Long) {
        val conversation = conversationRepository.getConversationWithMessages(conversationId)
        _messages.value = conversation?.messages?.map { msg ->
            MessageUi(
                id = msg.id,
                role = msg.role,
                content = msg.content,
                timestamp = msg.timestamp
            )
        } ?: emptyList()
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Loading : ChatUiState()
    object Processing : ChatUiState()
    object Success : ChatUiState()
    data class AudioProcessed(val result: AudioAnalysisResult) : ChatUiState()
    data class Error(val message: String) : ChatUiState()
}

data class MessageUi(
    val id: Long,
    val role: String,
    val content: String,
    val timestamp: Long
)
```

---

## æ€»ç»“

âœ… **AIé›†æˆæ¨¡å—å·²å®Œæˆ**ï¼ŒåŒ…æ‹¬ï¼š

1. **Qwen Dashscope API** - èŠå¤©ã€å®¢æˆ·åˆ†æžã€ä¼šè®®æ€»ç»“ã€CRMæ•°æ®æå–
2. **Qwen Tingwu API** - éŸ³é¢‘è½¬å†™ã€è¯´è¯äººåˆ†ç¦»ã€æ—¶é—´æˆ³ã€è‡ªåŠ¨æ€»ç»“
3. **æç¤ºè¯æ¨¡æ¿ç®¡ç†** - ç³»ç»Ÿæç¤ºè¯ã€å®¢æˆ·åˆ†æžã€ä¼šè®®æ€»ç»“ã€CRMæå–
4. **AiChatManager** - ä¸Šä¸‹æ–‡ç®¡ç†ã€å¤šè½®å¯¹è¯ã€ä¸“é¡¹åˆ†æž
5. **AudioTranscriptionManager** - ä»»åŠ¡åˆ›å»ºã€è¿›åº¦è½®è¯¢ã€ç»“æžœæ ¼å¼åŒ–
6. **AiRepository** - å®Œæ•´éŸ³é¢‘åˆ†æžæµç¨‹ï¼ˆè½¬å†™â†’åˆ†æžâ†’CRMæå–ï¼‰
7. **ViewModelç¤ºä¾‹** - UIçŠ¶æ€ç®¡ç†å’Œä½¿ç”¨ç¤ºä¾‹

## ä¸‹ä¸€æ­¥é€‰æ‹©ï¼š

3. **PDF/CSVå¯¼å‡ºæ¨¡å—** â†’ ç”ŸæˆCRMæ ¼å¼æ–‡æ¡£
4. **UIå±‚ (Jetpack Compose)** â†’ èŠå¤©ç•Œé¢ã€åŽ†å²è®°å½•ã€è®¾å¤‡é…å¯¹

ç»§ç»­å“ªä¸ªæ¨¡å—ï¼Ÿ

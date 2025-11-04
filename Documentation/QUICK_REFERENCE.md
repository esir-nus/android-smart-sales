# Smart Sales Assistant - 快速参考指南

## 🚀 核心代码片段

### 1. 数据库操作

#### 创建新对话
```kotlin
val conversationId = conversationRepository.createConversation("新对话")
```

#### 添加消息
```kotlin
conversationRepository.addMessage(
    conversationId = conversationId,
    role = "user",
    content = "消息内容",
    attachments = listOf(
        Attachment(
            type = "audio",
            path = "/path/to/file.mp3",
            name = "recording.mp3",
            size = 1024000,
            mimeType = "audio/mpeg"
        )
    )
)
```

#### 查询对话历史
```kotlin
conversationRepository.allConversations.collect { conversations ->
    // 处理对话列表
}
```

#### 按时间分组查询
```kotlin
val grouped = conversationRepository.getConversationsGroupedByTime()
// Result: Map<String, List<ConversationEntity>>
// 例如: {"7天内" -> [...], "30天内" -> [...], "2025年10月" -> [...]}
```

---

### 2. BLE设备连接

#### 扫描设备
```kotlin
// 开始扫描
bleManager.startScan()

// 观察发现的设备
bleManager.discoveredDevices.collect { devices ->
    devices.forEach { device ->
        println("发现设备: ${device.name} - ${device.address}")
    }
}

// 停止扫描
bleManager.stopScan()
```

#### 连接设备
```kotlin
bleManager.connectDevice(device)

// 观察连接状态
bleManager.connectionState.collect { state ->
    when (state) {
        is BleConnectionState.Disconnected -> println("未连接")
        is BleConnectionState.Connected -> println("已连接: ${state.device.name}")
        is BleConnectionState.ServicesDiscovered -> println("服务已发现")
    }
}
```

#### 发送WiFi配置
```kotlin
bleManager.sendWifiConfig(
    ssid = "MyWiFi",
    password = "password123"
).onSuccess {
    println("WiFi配置发送成功")
}.onFailure { e ->
    println("发送失败: ${e.message}")
}
```

#### 发送控制命令
```kotlin
bleManager.sendCommand(BleCommand.SYNC_FILES)
bleManager.sendCommand(BleCommand.UPDATE_IMAGE)
bleManager.sendCommand(BleCommand.REBOOT)
```

---

### 3. WiFi文件同步

#### 连接设备
```kotlin
fileSyncManager.connectToDevice("192.168.1.100")
    .onSuccess {
        println("连接成功")
    }
    .onFailure { e ->
        println("连接失败: ${e.message}")
    }
```

#### 同步所有文件
```kotlin
fileSyncManager.syncAllFiles()
    .onSuccess { files ->
        println("已同步 ${files.size} 个文件")
        files.forEach { file ->
            println("文件: ${file.name}")
        }
    }
```

#### 上传图片到设备
```kotlin
val imageFile = File("/path/to/image.png")
fileSyncManager.uploadImageToDevice(imageFile)
    .onSuccess {
        println("图片上传成功")
    }
```

#### 更新设备显示文字
```kotlin
fileSyncManager.updateDeviceText("销售中")
    .onSuccess {
        println("文字更新成功")
    }
```

#### 获取设备信息
```kotlin
fileSyncManager.getDeviceInfo()
    .onSuccess { info ->
        println("设备: ${info.deviceName}")
        println("电池: ${info.batteryLevel}%")
        println("存储: ${info.storageUsed}/${info.storageTotal}")
    }
```

---

### 4. AI聊天

#### 普通聊天
```kotlin
aiServiceManager.sendChatMessage(
    conversationId = conversationId,
    userMessage = "如何提高销售转化率？",
    mode = ChatMode.NORMAL
).onSuccess { response ->
    println("AI回复: $response")
}
```

#### 流式聊天（实时响应）
```kotlin
var fullResponse = ""
aiServiceManager.sendChatMessageStream(
    conversationId = conversationId,
    userMessage = "分析这个客户",
    onChunk = { chunk ->
        fullResponse += chunk
        // 实时更新UI
        updateUI(fullResponse)
    }
)
```

#### 客户分析
```kotlin
val transcript = """
客户: 我们公司想采购100台设备...
销售: 请问您的预算范围是多少？
"""

aiServiceManager.analyzeCustomer(
    conversationId = conversationId,
    audioTranscript = transcript,
    additionalNotes = "客户是500强企业采购总监"
).onSuccess { analysis ->
    println(analysis) // Markdown格式的分析报告
}
```

#### CRM数据提取
```kotlin
aiServiceManager.extractCrmData(
    conversationId = conversationId,
    transcript = transcript
).onSuccess { jsonData ->
    val customerData = CustomerData.fromJson(jsonData)
    println("客户姓名: ${customerData?.name}")
    println("公司: ${customerData?.company}")
}
```

---

### 5. 音频转写

#### 提交转写任务
```kotlin
val audioFile = File("/path/to/meeting.mp3")
val taskId = tingwuClient.submitTranscription(
    audioFile = audioFile,
    enableDiarization = true,  // 启用说话人分离
    enableSummarization = true  // 启用自动摘要
).getOrNull()
```

#### 等待转写完成
```kotlin
tingwuClient.waitForTranscription(
    taskId = taskId!!,
    onProgress = { status ->
        println("转写状态: $status")
    }
).onSuccess { result ->
    // 获取转写文本
    val fullText = result.transcription?.text
    
    // 获取分段内容
    result.transcription?.segments?.forEach { segment ->
        println("[${segment.startTime}ms] ${segment.speakerId}: ${segment.text}")
    }
    
    // 获取说话人信息
    result.diarization?.speakers?.forEach { speaker ->
        println("说话人 ${speaker.speakerId}:")
        speaker.segments.forEach { segment ->
            println("  - ${segment.text}")
        }
    }
    
    // 获取摘要
    val summary = result.summarization?.summary
    val keyPoints = result.summarization?.keyPoints
}
```

---

### 6. PDF导出

#### 导出客户分析报告
```kotlin
exportManager.exportCustomerAnalysisPdf(
    conversationId = conversationId,
    customerName = "张三"
).onSuccess { file ->
    println("PDF已生成: ${file.absolutePath}")
    
    // 分享文件
    val intent = exportManager.shareFile(file, "application/pdf")
    startActivity(Intent.createChooser(intent, "分享PDF"))
}
```

#### 导出会议纪要
```kotlin
exportManager.exportMeetingMinutesPdf(
    conversationId = conversationId,
    meetingTitle = "Q4产品规划会议",
    participants = "张三, 李四, 王五",
    transcript = meetingTranscript
).onSuccess { file ->
    println("会议纪要已生成")
}
```

---

### 7. CSV导出

#### 单个对话导出
```kotlin
exportManager.exportCrmDataCsv(
    conversationId = conversationId,
    format = CrmFormat.SALESFORCE  // 或 HUBSPOT, GENERIC
).onSuccess { file ->
    println("CSV已生成: ${file.absolutePath}")
}
```

#### 批量导出
```kotlin
val conversationIds = listOf(1L, 2L, 3L, 4L, 5L)
exportManager.exportBatchCrmData(
    conversationIds = conversationIds,
    format = CrmFormat.GENERIC
).onSuccess { file ->
    println("批量导出完成，包含 ${conversationIds.size} 个对话")
}
```

#### 导出对话记录
```kotlin
exportManager.exportConversationCsv(conversationId)
    .onSuccess { file ->
        println("对话记录已导出为CSV")
    }
```

---

### 8. Compose UI模式

#### ViewModel观察状态
```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val messages by viewModel.messages.collectAsState()
    val chatState by viewModel.chatState.collectAsState()
    
    when (chatState) {
        is ChatState.Idle -> { /* 空闲状态 */ }
        is ChatState.Processing -> { /* 显示加载动画 */ }
        is ChatState.Completed -> { /* 完成 */ }
        is ChatState.Error -> { 
            val error = (chatState as ChatState.Error).message
            /* 显示错误 */
        }
    }
}
```

#### LazyColumn with StateFlow
```kotlin
@Composable
fun ConversationList(viewModel: HistoryViewModel) {
    val conversations by viewModel.groupedConversations.collectAsState()
    
    LazyColumn {
        conversations.forEach { (timeGroup, items) ->
            stickyHeader {
                Text(text = timeGroup)
            }
            items(items) { conversation ->
                ConversationItem(conversation)
            }
        }
    }
}
```

#### 权限请求
```kotlin
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler() {
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    
    if (permissionsState.allPermissionsGranted) {
        // 权限已授予
        BluetoothScreen()
    } else {
        // 请求权限
        Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
            Text("授予权限")
        }
    }
}
```

---

## 🔧 实用工具函数

### 图片处理
```kotlin
// 验证图片
val result = ImageProcessor.validateImage(imageFile)
when (result) {
    is ImageValidationResult.Valid -> { /* 图片符合要求 */ }
    is ImageValidationResult.NeedsResize -> { /* 需要调整大小 */ }
    is ImageValidationResult.Error -> { /* 错误 */ }
}

// 转换图片格式
ImageProcessor.convertToGadgetFormat(
    inputFile = originalFile,
    outputFile = targetFile,
    format = Bitmap.CompressFormat.PNG
)

// 从URI加载并处理
ImageProcessor.loadAndProcessFromUri(
    context = context,
    uri = imageUri,
    outputFile = outputFile
)

// 创建文字图片
val textBitmap = ImageProcessor.createTextImage(
    text = "录音中",
    backgroundColor = Color.BLACK,
    textColor = Color.WHITE,
    textSize = 24f
)
```

### 时间格式化
```kotlin
// 相对时间
formatTimestamp(timestamp)
// 输出: "刚刚", "5分钟前", "10:30", "11-03 14:20"

// 完整日期
SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.CHINESE).format(Date())
// 输出: "2025年11月03日 14:30"
```

### Markdown处理
```kotlin
// 简单Markdown解析
val lines = parseMarkdownToPdfLines(markdownText)
// 返回: List<PdfLine> with LineType (HEADING1, HEADING2, BULLET, NORMAL, BOLD)

// Compose中渲染Markdown
@Composable
fun MarkdownText(markdown: String, color: Color) {
    // 自动处理 **加粗** 等格式
}
```

---

## 📊 常用数据结构

### 消息模型
```kotlin
data class MessageEntity(
    val id: Long = 0,
    val conversationId: Long,
    val role: String,           // "user" | "assistant"
    val content: String,
    val timestamp: Long,
    val attachments: String?,   // JSON: [{"type":"audio","path":"..."}]
    val metadata: String?       // JSON: {"tokens":1234,"model":"qwen-max"}
)
```

### 对话模型
```kotlin
data class ConversationEntity(
    val id: Long = 0,
    val title: String,
    val summary: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val isAnalyzed: Boolean = false
)
```

### 设备信息
```kotlin
data class DeviceInfoResponse(
    val success: Boolean,
    val deviceId: String,
    val deviceName: String,
    val firmwareVersion: String,
    val batteryLevel: Int,
    val storageUsed: Long,
    val storageTotal: Long
)
```

### 转写结果
```kotlin
data class TranscriptionResult(
    val taskId: String,
    val status: String,
    val transcription: Transcription?,    // 转写文本
    val diarization: Diarization?,        // 说话人分离
    val summarization: Summarization?     // 摘要
)
```

---

## 🎯 设计模式应用

### Repository模式
```kotlin
// Repository作为数据访问的唯一入口
class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    // 对外提供简洁的接口
    suspend fun createConversation(title: String): Long
    suspend fun addMessage(...)
    suspend fun deleteConversation(id: Long)
}

// ViewModel使用Repository
class ChatViewModel @Inject constructor(
    private val repository: ConversationRepository
) : ViewModel() {
    // 业务逻辑
}
```

### Observer模式 (Flow)
```kotlin
// 数据层发射数据
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations")
    fun getAllConversations(): Flow<List<ConversationEntity>>
}

// UI层订阅数据
viewModel.conversations.collectAsState()
```

### Singleton模式 (Hilt)
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartSalesDatabase
}
```

### Strategy模式
```kotlin
// 不同的CRM格式策略
enum class CrmFormat {
    SALESFORCE,
    HUBSPOT,
    GENERIC
}

fun mapCustomerToRow(customer: CustomerData, format: CrmFormat): List<String> {
    return when (format) {
        CrmFormat.SALESFORCE -> salesforceMapping(customer)
        CrmFormat.HUBSPOT -> hubspotMapping(customer)
        CrmFormat.GENERIC -> genericMapping(customer)
    }
}
```

---

## ⚡ 性能优化技巧

### 1. 数据库查询优化
```kotlin
// ✅ 使用索引
@Entity(indices = [Index(value = ["conversationId"])])
data class MessageEntity(...)

// ✅ 分页查询
@Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
suspend fun getConversations(limit: Int, offset: Int): List<ConversationEntity>

// ✅ 使用Flow自动更新
fun getAllConversations(): Flow<List<ConversationEntity>>
```

### 2. Compose优化
```kotlin
// ✅ 使用key避免重组
LazyColumn {
    items(messages, key = { it.id }) { message ->
        MessageBubble(message)
    }
}

// ✅ 使用remember缓存计算
val formattedDate = remember(timestamp) {
    formatTimestamp(timestamp)
}

// ✅ 使用derivedStateOf避免过度重组
val hasMessages by remember {
    derivedStateOf { messages.isNotEmpty() }
}
```

### 3. 网络请求优化
```kotlin
// ✅ 使用连接池
private val client = OkHttpClient.Builder()
    .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
    .build()

// ✅ 设置合理的超时
.connectTimeout(10, TimeUnit.SECONDS)
.readTimeout(30, TimeUnit.SECONDS)

// ✅ 添加重试机制
suspend fun <T> retryIO(
    times: Int = 3,
    delay: Long = 1000,
    block: suspend () -> T
): T {
    repeat(times - 1) {
        try {
            return block()
        } catch (e: IOException) {
            delay(delay)
        }
    }
    return block()
}
```

### 4. 内存管理
```kotlin
// ✅ 及时回收Bitmap
bitmap.recycle()

// ✅ 使用WeakReference
private val imageCache = WeakHashMap<String, Bitmap>()

// ✅ 限制缓存大小
private val lruCache = LruCache<String, Bitmap>(
    (Runtime.getRuntime().maxMemory() / 8).toInt()
)
```

---

## 🔍 调试技巧

### 1. 日志记录
```kotlin
// 使用Timber
Timber.d("Debug message")
Timber.e(exception, "Error occurred")
Timber.tag("BLE").i("Device connected: %s", deviceName)
```

### 2. 数据库检查
```kotlin
// 使用Android Studio的Database Inspector
// View -> Tool Windows -> App Inspection -> Database Inspector

// 或使用ADB
adb shell
run-as com.example.smartsales
cd databases
sqlite3 smart_sales_database
```

### 3. 网络抓包
```kotlin
// 添加Logging Interceptor
val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BODY
}

OkHttpClient.Builder()
    .addInterceptor(logging)
    .build()
```

---

## 📱 生产环境检查清单

- [ ] 移除所有调试日志
- [ ] 配置正确的API Key
- [ ] 启用ProGuard混淆
- [ ] 实现错误上报 (Crashlytics)
- [ ] 添加性能监控
- [ ] 测试所有权限流程
- [ ] 验证数据库迁移
- [ ] 检查内存泄漏
- [ ] 测试低端设备性能
- [ ] 验证网络异常处理
- [ ] 测试BLE连接稳定性
- [ ] 检查文件存储限制
- [ ] 验证数据加密
- [ ] 测试离线功能
- [ ] 准备发布签名

---

**最后更新**: 2025年11月03日  
**版本**: 1.0.0

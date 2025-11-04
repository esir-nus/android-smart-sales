# 最佳实践与已知问题 | Best Practices & Known Issues

## 📋 文档说明
本文档记录项目开发过程中的最佳实践、踩坑经验和已知问题，持续更新。

**更新频率**: 每次遇到新问题或找到更好解决方案时立即更新

---

## 🎯 Kotlin最佳实践

### 1. 协程使用
✅ **正确做法**
```kotlin
// 使用viewModelScope，自动取消
class MyViewModel : ViewModel() {
    fun loadData() {
        viewModelScope.launch {
            repository.getData()
        }
    }
}

// 使用withContext切换线程
suspend fun processImage(bitmap: Bitmap) = withContext(Dispatchers.Default) {
    // CPU密集型任务
    compressBitmap(bitmap)
}
```

❌ **错误做法**
```kotlin
// 使用GlobalScope，可能内存泄漏
GlobalScope.launch {
    repository.getData()
}

// 阻塞主线程
runBlocking {
    processImage(bitmap)
}
```

**已知问题**: 
- `viewModelScope`在配置变更时不会取消，需要在`onCleared()`手动处理长时间任务
- **解决方案**: 使用`Job`引用并在`onCleared()`中`cancel()`

---

### 2. Flow使用

✅ **正确做法**
```kotlin
// 使用StateFlow暴露状态
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()

// 在UI层收集
LaunchedEffect(Unit) {
    viewModel.uiState.collect { state ->
        // 更新UI
    }
}
```

❌ **错误做法**
```kotlin
// 使用LiveData (与Compose集成不佳)
val uiState = MutableLiveData<UiState>()

// 在Composable中直接launch (可能泄漏)
LaunchedEffect(Unit) {
    viewModel.someFlow.collect { }
}
```

**已知问题**: 
- `SharedFlow`默认不缓存值，新订阅者收不到历史值
- **解决方案**: 使用`StateFlow`或配置`replay=1`

---

### 3. 空安全

✅ **正确做法**
```kotlin
// 使用安全调用和Elvis操作符
val result = data?.field ?: "default"

// 使用let处理可空值
data?.let { nonNullData ->
    processData(nonNullData)
}
```

❌ **错误做法**
```kotlin
// 强制非空断言 (可能崩溃)
val result = data!!.field

// 过度使用if判断
if (data != null) {
    if (data.field != null) {
        processData(data.field!!)
    }
}
```

---

## 🔵 BLE开发最佳实践

### 1. FastBle初始化

✅ **正确做法**
```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        BleManager.getInstance().init(this)
        BleManager.getInstance().apply {
            enableLog(BuildConfig.DEBUG)
            setReConnectCount(3, 3000)
            setOperateTimeout(10000)
        }
    }
}
```

**已知问题**: 
- FastBle必须在Application中初始化，否则会空指针
- **解决方案**: 在`Application.onCreate()`中初始化

---

### 2. BLE扫描

✅ **正确做法**
```kotlin
fun scanDevices() {
    BleManager.getInstance().scan(object : BleScanCallback() {
        override fun onScanStarted(success: Boolean) {
            if (success) {
                Timber.d("[BLE][scan] 扫描已开始")
            }
        }

        override fun onScanning(bleDevice: BleDevice) {
            Timber.v("[BLE][scan] 发现设备: ${bleDevice.name}, RSSI: ${bleDevice.rssi}")
            // 过滤信号强度
            if (bleDevice.rssi > -70) {
                // 添加到列表
            }
        }

        override fun onScanFinished(scanResultList: List<BleDevice>) {
            Timber.d("[BLE][scan] 扫描完成，共发现 ${scanResultList.size} 个设备")
        }
    })
}
```

**已知问题**:
1. **Android 12+需要BLUETOOTH_SCAN权限**
   ```kotlin
   if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
       requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN))
   }
   ```

2. **部分手机扫描不到设备**
   - 原因: 厂商定制系统限制后台扫描
   - **解决方案**: 在前台扫描，并提示用户保持屏幕开启

3. **扫描结果重复**
   - 原因: `onScanning`可能多次回调同一设备
   - **解决方案**: 使用`Map<String, BleDevice>`去重 (以MAC地址为key)

---

### 3. BLE连接

✅ **正确做法**
```kotlin
fun connectDevice(bleDevice: BleDevice) {
    BleManager.getInstance().connect(bleDevice, object : BleGattCallback() {
        override fun onStartConnect() {
            Timber.d("[BLE][connect] 开始连接: ${bleDevice.mac}")
        }

        override fun onConnectSuccess(bleDevice: BleDevice, gatt: BluetoothGatt, status: Int) {
            Timber.i("[BLE][connect] 连接成功: ${bleDevice.mac}")
            // 启用通知
            enableNotification(bleDevice)
        }

        override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
            Timber.e("[BLE][connect] 连接失败: ${exception.description}")
            handleConnectionError(exception)
        }

        override fun onDisConnected(isActiveDisConnected: Boolean, device: BleDevice, gatt: BluetoothGatt, status: Int) {
            Timber.w("[BLE][connect] 断开连接，主动断开: $isActiveDisConnected")
            if (!isActiveDisConnected) {
                // 非主动断开，尝试重连
                reconnect(device)
            }
        }
    })
}
```

**已知问题**:
1. **连接后立即写入数据失败**
   - 原因: 服务发现需要时间 (500ms-2s)
   - **解决方案**: 在`onConnectSuccess`中延迟500ms再写入
   ```kotlin
   override fun onConnectSuccess(...) {
       viewModelScope.launch {
           delay(500)
           writeData()
       }
   }
   ```

2. **连接状态133错误**
   - 原因: GATT连接失败 (可能是距离太远/信号干扰)
   - **解决方案**: 
     - 提示用户靠近设备
     - 关闭并重新打开蓝牙
     - 最多重试3次

3. **连接后无法通信**
   - 原因: 未启用Notify/Indicate特征
   - **解决方案**: 连接成功后立即启用通知
   ```kotlin
   BleManager.getInstance().notify(
       bleDevice,
       serviceUuid,
       notifyUuid,
       object : BleNotifyCallback() {
           override fun onNotifySuccess() {
               Timber.d("[BLE][notify] 通知已启用")
           }
       }
   )
   ```

---

### 4. BLE数据传输

✅ **正确做法**
```kotlin
fun sendWifiConfig(ssid: String, password: String) {
    val json = JSONObject().apply {
        put("ssid", ssid)
        put("password", password)
    }.toString()
    
    val data = json.toByteArray(Charsets.UTF_8)
    
    Timber.d("[BLE][write] 发送数据: $json, 长度: ${data.size} bytes")
    
    BleManager.getInstance().write(
        bleDevice,
        BleConstants.SERVICE_UUID,
        BleConstants.WIFI_CONFIG_CHAR,
        data,
        object : BleWriteCallback() {
            override fun onWriteSuccess(current: Int, total: Int, justWrite: ByteArray) {
                Timber.i("[BLE][write] 写入成功: $current/$total")
                if (current == total) {
                    Timber.i("[BLE][write] 全部数据已发送")
                }
            }

            override fun onWriteFailure(exception: BleException) {
                Timber.e("[BLE][write] 写入失败: ${exception.description}")
            }
        }
    )
}
```

**已知问题**:
1. **单次写入数据过大失败**
   - 原因: BLE MTU限制 (默认20-23字节)
   - **解决方案**: 
     - 请求更大MTU: `BleManager.getInstance().setMtu(bleDevice, 512)`
     - 或分包发送

2. **中文乱码**
   - 原因: 字符编码不一致
   - **解决方案**: 统一使用UTF-8编码
   ```kotlin
   val data = text.toByteArray(Charsets.UTF_8)
   ```

3. **写入速度慢**
   - 原因: 默认写入间隔较长
   - **解决方案**: 调整写入间隔
   ```kotlin
   BleManager.getInstance().setSplitWriteNum(20) // 每包20字节
   ```

---

## 🌐 网络通信最佳实践

### 1. Retrofit配置

✅ **正确做法**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "SalesAssistant/1.0")
                    .build()
                chain.proceed(request)
            }
            .retryOnConnectionFailure(true)
            .build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // 动态替换
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}
```

**已知问题**:
1. **Retrofit baseUrl不能动态修改**
   - 原因: Retrofit创建时baseUrl固定
   - **解决方案**: 使用`@Url`注解传入完整URL
   ```kotlin
   @GET
   suspend fun getFileList(@Url url: String): FileListResponse
   
   // 调用时
   apiService.getFileList("http://$gadgetIp:8080/api/files/list")
   ```

2. **HTTPS证书验证失败**
   - 原因: 自签名证书
   - **解决方案**: 开发环境跳过证书验证 (生产环境禁用)
   ```kotlin
   val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
       override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
       override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
       override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
   })
   ```

---

### 2. 文件下载

✅ **正确做法**
```kotlin
suspend fun downloadFile(url: String, destFile: File): Result<File> {
    return withContext(Dispatchers.IO) {
        try {
            val response = apiService.downloadFile(url)
            val body = response.body() ?: return@withContext Result.failure(Exception("响应体为空"))
            
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            
            body.byteStream().use { input ->
                destFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        val progress = (downloadedBytes * 100 / totalBytes).toInt()
                        Timber.v("[Download] 进度: $progress%")
                        // 发送进度事件
                    }
                }
            }
            
            Result.success(destFile)
        } catch (e: Exception) {
            Timber.e(e, "[Download] 下载失败")
            Result.failure(e)
        }
    }
}
```

**已知问题**:
1. **大文件下载内存溢出**
   - 原因: 一次性加载到内存
   - **解决方案**: 使用`@Streaming`注解流式下载
   ```kotlin
   @Streaming
   @GET
   suspend fun downloadFile(@Url url: String): Response<ResponseBody>
   ```

2. **下载中断后无法续传**
   - 原因: 未使用Range请求头
   - **解决方案**: 添加断点续传逻辑
   ```kotlin
   val request = Request.Builder()
       .url(url)
       .addHeader("Range", "bytes=$downloadedBytes-")
       .build()
   ```

---

## 🤖 Qwen API最佳实践

### 1. 流式响应处理

✅ **正确做法**
```kotlin
suspend fun chatStream(messages: List<ChatMessage>): Flow<String> = flow {
    val request = ChatRequest(
        model = "qwen-max",
        input = ChatInput(messages),
        parameters = ChatParameters(
            resultFormat = "message",
            incrementalOutput = true // 启用流式输出
        )
    )
    
    val response = apiService.chatStream(request)
    val reader = response.body()?.charStream() ?: return@flow
    
    reader.useLines { lines ->
        lines.forEach { line ->
            if (line.startsWith("data:")) {
                val json = line.removePrefix("data:").trim()
                if (json == "[DONE]") return@forEach
                
                val data = parseJson(json)
                val content = data.output.choices[0].message.content
                emit(content)
            }
        }
    }
}.flowOn(Dispatchers.IO)
```

**已知问题**:
1. **SSE解析失败**
   - 原因: Retrofit不原生支持SSE
   - **解决方案**: 使用OkHttp直接请求
   ```kotlin
   val request = Request.Builder()
       .url(apiUrl)
       .addHeader("Accept", "text/event-stream")
       .post(requestBody)
       .build()
   
   val response = okHttpClient.newCall(request).execute()
   ```

2. **API限流429错误**
   - 原因: 调用频率过高
   - **解决方案**: 实现指数退避重试
   ```kotlin
   var delay = 1000L
   repeat(3) { attempt ->
       try {
           return apiService.chat(request)
       } catch (e: HttpException) {
           if (e.code() == 429) {
               delay(delay)
               delay *= 2
           } else throw e
       }
   }
   ```

---

### 2. 音频上传

✅ **正确做法**
```kotlin
suspend fun uploadAudio(
    audioFile: File,
    images: List<File>? = null,
    text: String? = null
): TranscriptionTask {
    val audioPart = MultipartBody.Part.createFormData(
        "audio",
        audioFile.name,
        audioFile.asRequestBody("audio/mp3".toMediaTypeOrNull())
    )
    
    val imageParts = images?.map { file ->
        MultipartBody.Part.createFormData(
            "images",
            file.name,
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )
    }
    
    val textBody = text?.let {
        RequestBody.create("text/plain".toMediaTypeOrNull(), it)
    }
    
    Timber.d("[Tingwu] 上传音频: ${audioFile.name}, 大小: ${audioFile.length()} bytes")
    
    return apiService.transcribe(
        apiKey = API_KEY,
        audio = audioPart,
        images = imageParts,
        text = textBody
    )
}
```

**已知问题**:
1. **大音频文件上传超时**
   - 原因: 30秒超时不够
   - **解决方案**: 动态调整超时时间
   ```kotlin
   val fileSize = audioFile.length()
   val timeout = (fileSize / 1024 / 100).coerceAtLeast(30) // 100KB/s估算
   
   val client = okHttpClient.newBuilder()
       .writeTimeout(timeout, TimeUnit.SECONDS)
       .build()
   ```

2. **上传进度无法获取**
   - 原因: Retrofit不支持上传进度
   - **解决方案**: 自定义RequestBody
   ```kotlin
   class ProgressRequestBody(
       private val delegate: RequestBody,
       private val onProgress: (Long, Long) -> Unit
   ) : RequestBody() {
       override fun contentType() = delegate.contentType()
       override fun contentLength() = delegate.contentLength()
       
       override fun writeTo(sink: BufferedSink) {
           val total = contentLength()
           var uploaded = 0L
           
           val source = delegate.source()
           var read: Long
           while (source.read(sink.buffer, 8192).also { read = it } != -1L) {
               uploaded += read
               onProgress(uploaded, total)
           }
       }
   }
   ```

---

## 🗄️ 数据库最佳实践

### 1. Room配置

✅ **正确做法**
```kotlin
@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}

// Hilt提供
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
    return Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "sales_assistant.db"
    )
    .fallbackToDestructiveMigration() // 开发阶段
    .build()
}
```

**已知问题**:
1. **主线程访问数据库崩溃**
   - 原因: Room不允许主线程操作
   - **解决方案**: 使用`suspend`函数或`Flow`
   ```kotlin
   @Query("SELECT * FROM conversations")
   fun getAllConversations(): Flow<List<ConversationEntity>> // ✅
   
   @Query("SELECT * FROM conversations")
   suspend fun getAllConversations(): List<ConversationEntity> // ✅
   ```

2. **数据库升级失败**
   - 原因: 未提供Migration
   - **解决方案**: 开发阶段使用`fallbackToDestructiveMigration()`
   ```kotlin
   // 生产环境提供Migration
   val MIGRATION_1_2 = object : Migration(1, 2) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL("ALTER TABLE conversations ADD COLUMN summary TEXT")
       }
   }
   ```

---

### 2. 大量数据插入

✅ **正确做法**
```kotlin
@Dao
interface MessageDao {
    @Insert
    suspend fun insertAll(messages: List<MessageEntity>)
    
    @Transaction
    suspend fun replaceMessages(conversationId: Long, newMessages: List<MessageEntity>) {
        deleteByConversation(conversationId)
        insertAll(newMessages)
    }
}
```

**已知问题**:
1. **批量插入慢**
   - 原因: 逐条插入效率低
   - **解决方案**: 使用`@Insert`批量插入，并在事务中执行
   ```kotlin
   @Transaction
   suspend fun batchInsert(messages: List<MessageEntity>) {
       messages.chunked(100).forEach { chunk ->
           insertAll(chunk)
       }
   }
   ```

---

## 🎨 Compose UI最佳实践

### 1. 状态提升

✅ **正确做法**
```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    ChatContent(
        messages = uiState.messages,
        onSendMessage = { text -> viewModel.sendMessage(text) }
    )
}

@Composable
fun ChatContent(
    messages: List<Message>,
    onSendMessage: (String) -> Unit
) {
    // 无状态组件
}
```

❌ **错误做法**
```kotlin
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(emptyList<Message>()) }
    // 状态和逻辑混在一起
}
```

---

### 2. LazyColumn优化

✅ **正确做法**
```kotlin
@Composable
fun MessageList(messages: List<Message>) {
    LazyColumn(
        reverseLayout = true, // 从底部开始
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = messages,
            key = { it.id } // 提供唯一key
        ) { message ->
            MessageBubble(message)
        }
    }
}
```

**已知问题**:
1. **滚动卡顿**
   - 原因: item内容过于复杂
   - **解决方案**: 
     - 使用`key`参数避免重组
     - 简化item布局
     - 使用`derivedStateOf`减少重组

2. **自动滚动到底部失效**
   - 原因: 使用`reverseLayout`后逻辑相反
   - **解决方案**:
   ```kotlin
   val listState = rememberLazyListState()
   
   LaunchedEffect(messages.size) {
       if (messages.isNotEmpty()) {
           listState.animateScrollToItem(0) // reverseLayout下0是最新
       }
   }
   ```

---

### 3. 图片加载

✅ **正确做法**
```kotlin
@Composable
fun ProfileImage(url: String) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .size(80, 80) // 指定尺寸，避免加载原图
            .build(),
        contentDescription = "头像",
        modifier = Modifier.size(80.dp)
    )
}
```

**已知问题**:
1. **大图OOM**
   - 原因: 加载原图到内存
   - **解决方案**: 使用Coil的`size()`指定目标尺寸

---

## 🐛 常见Bug及解决方案

### 1. BLE连接后无法写入数据
**现象**: `onConnectSuccess`回调后立即写入失败

**原因**: 服务发现需要时间

**解决方案**:
```kotlin
override fun onConnectSuccess(...) {
    viewModelScope.launch {
        delay(500) // 等待服务发现完成
        writeData()
    }
}
```

---

### 2. 协程取消后仍在执行
**现象**: ViewModel销毁后协程还在运行

**原因**: 使用了`GlobalScope`或未传播取消

**解决方案**:
```kotlin
// 使用viewModelScope
viewModelScope.launch {
    // 自动取消
}

// 手动Job管理
private val job = Job()
private val scope = CoroutineScope(Dispatchers.Main + job)

override fun onCleared() {
    job.cancel()
}
```

---

### 3. Compose界面不更新
**现象**: ViewModel状态改变但UI不刷新

**原因**: 使用了非可观察状态

**解决方案**:
```kotlin
// ❌ 错误
var uiState = UiState()

// ✅ 正确
private val _uiState = MutableStateFlow(UiState())
val uiState: StateFlow<UiState> = _uiState.asStateFlow()
```

---

### 4. 中文乱码
**现象**: API返回或BLE传输的中文显示乱码

**原因**: 字符编码不一致

**解决方案**:
```kotlin
// 统一使用UTF-8
val data = text.toByteArray(Charsets.UTF_8)
val text = data.toString(Charsets.UTF_8)

// CSV文件添加BOM
writer.write("\uFEFF") // UTF-8 BOM for Excel
```

---

### 5. APK体积过大
**现象**: Release APK超过50MB

**原因**: 包含未使用的资源和库

**解决方案**:
```gradle
android {
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

---

## 📊 性能优化检查清单

- [ ] 启动速度: 使用`App Startup`延迟初始化
- [ ] 内存: 使用LeakCanary检测泄漏
- [ ] 布局: 减少嵌套层级，使用`ConstraintLayout`
- [ ] 图片: 使用WebP格式，按需加载
- [ ] 数据库: 为常查询字段添加索引
- [ ] 网络: 启用GZIP压缩
- [ ] 后台任务: 使用WorkManager替代Service

---

## 🔄 持续更新

**最后更新**: 2025-11-03

**待解决问题**:
1. 多设备BLE冲突
2. Qwen API成本优化
3. 离线模式实现

**计划改进**:
1. 添加单元测试覆盖率监控
2. 集成Crashlytics崩溃报告
3. 实现A/B测试框架

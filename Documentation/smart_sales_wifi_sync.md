# Smart Sales Assistant - WiFi File Sync Module

## 3.6 WiFiæ–‡ä»¶åŒæ­¥æ¨¡å—

### 3.6.1 HTTP APIå®šä¹‰

```kotlin
// APIç«¯ç‚¹æŽ¥å£
interface GadgetApi {
    // èŽ·å–æ–‡ä»¶åˆ—è¡¨
    @GET("/api/files/list")
    suspend fun getFileList(): Response<FileListResponse>
    
    // ä¸‹è½½æ–‡ä»¶
    @GET("/api/files/download")
    @Streaming
    suspend fun downloadFile(@Query("filename") filename: String): Response<ResponseBody>
    
    // ä¸Šä¼ å›¾ç‰‡åˆ°è®¾å¤‡
    @Multipart
    @POST("/api/display/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): Response<UploadResponse>
    
    // æ›´æ–°æ˜¾ç¤ºæ–‡å­—
    @POST("/api/display/text")
    suspend fun updateDisplayText(@Body request: TextDisplayRequest): Response<BaseResponse>
    
    // èŽ·å–è®¾å¤‡çŠ¶æ€
    @GET("/api/device/status")
    suspend fun getDeviceStatus(): Response<DeviceStatusResponse>
    
    // åˆ é™¤è®¾å¤‡æ–‡ä»¶
    @DELETE("/api/files/delete")
    suspend fun deleteFile(@Query("filename") filename: String): Response<BaseResponse>
}

// æ•°æ®æ¨¡åž‹
data class FileListResponse(
    val success: Boolean,
    val files: List<GadgetFile>
)

data class GadgetFile(
    val filename: String,
    val type: String,          // "audio" | "image"
    val size: Long,
    val createdAt: Long,
    val path: String
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val filename: String?
)

data class TextDisplayRequest(
    val text: String,          // æœ€å¤š10ä¸ªå­—ç¬¦
    val duration: Int? = null  // æ˜¾ç¤ºæ—¶é•¿ï¼ˆç§’ï¼‰ï¼Œnullè¡¨ç¤ºæŒç»­æ˜¾ç¤º
)

data class DeviceStatusResponse(
    val success: Boolean,
    val deviceId: String,
    val batteryLevel: Int,     // 0-100
    val storageUsed: Long,     // å­—èŠ‚
    val storageTotal: Long,    // å­—èŠ‚
    val currentImage: String?,
    val currentText: String?,
    val wifiConnected: Boolean,
    val recordingCount: Int,
    val imageCount: Int
)

data class BaseResponse(
    val success: Boolean,
    val message: String
)
```

---

### 3.6.2 Retrofité…ç½®

```kotlin
object GadgetApiClient {
    private var retrofit: Retrofit? = null
    private var baseUrl: String = "http://192.168.4.1:8080" // é»˜è®¤APæ¨¡å¼åœ°å€
    
    // é…ç½®åŸºç¡€URLï¼ˆè®¾å¤‡IPåœ°å€ï¼‰
    fun configure(deviceIpAddress: String, port: Int = 8080) {
        baseUrl = "http://$deviceIpAddress:$port"
        retrofit = null // é‡ç½®ä»¥ä½¿ç”¨æ–°URL
    }
    
    private fun getRetrofit(): Retrofit {
        return retrofit ?: synchronized(this) {
            val instance = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(createOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            retrofit = instance
            instance
        }
    }
    
    private fun createOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor())
            .addInterceptor(ErrorInterceptor())
            .build()
    }
    
    fun getApi(): GadgetApi {
        return getRetrofit().create(GadgetApi::class.java)
    }
}

// æ—¥å¿—æ‹¦æˆªå™¨
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val startTime = System.currentTimeMillis()
        
        Log.d("HttpClient", "Request: ${request.method} ${request.url}")
        
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime
        
        Log.d("HttpClient", "Response: ${response.code} in ${duration}ms")
        
        return response
    }
}

// é”™è¯¯æ‹¦æˆªå™¨
class ErrorInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        try {
            val response = chain.proceed(request)
            
            if (!response.isSuccessful) {
                Log.e("HttpClient", "HTTP Error: ${response.code} ${response.message}")
            }
            
            return response
        } catch (e: Exception) {
            Log.e("HttpClient", "Network Error: ${e.message}")
            throw e
        }
    }
}
```

---

### 3.6.3 æ–‡ä»¶åŒæ­¥ç®¡ç†å™¨

```kotlin
class FileSyncManager(
    private val context: Context,
    private val api: GadgetApi,
    private val attachmentDao: AttachmentDao
) {
    private val _syncProgress = MutableStateFlow<SyncProgress>(SyncProgress.Idle)
    val syncProgress: StateFlow<SyncProgress> = _syncProgress.asStateFlow()
    
    private val downloadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // æœ¬åœ°å­˜å‚¨ç›®å½•
    private val audioDir = File(context.filesDir, "audio")
    private val imageDir = File(context.filesDir, "images")
    
    init {
        audioDir.mkdirs()
        imageDir.mkdirs()
    }
    
    // åŒæ­¥æ–‡ä»¶åˆ—è¡¨
    suspend fun syncFileList(): Result<List<GadgetFile>> {
        return withContext(Dispatchers.IO) {
            try {
                _syncProgress.value = SyncProgress.Fetching
                
                val response = api.getFileList()
                if (response.isSuccessful && response.body()?.success == true) {
                    val files = response.body()!!.files
                    _syncProgress.value = SyncProgress.Success(files.size)
                    Result.success(files)
                } else {
                    _syncProgress.value = SyncProgress.Error("Failed to fetch file list")
                    Result.failure(Exception("API Error: ${response.code()}"))
                }
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress.Error(e.message ?: "Unknown error")
                Result.failure(e)
            }
        }
    }
    
    // ä¸‹è½½å•ä¸ªæ–‡ä»¶
    suspend fun downloadFile(
        gadgetFile: GadgetFile,
        onProgress: (Int) -> Unit = {}
    ): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.downloadFile(gadgetFile.filename)
                
                if (response.isSuccessful) {
                    val body = response.body()!!
                    val targetDir = if (gadgetFile.type == "audio") audioDir else imageDir
                    val targetFile = File(targetDir, gadgetFile.filename)
                    
                    // æµå¼ä¸‹è½½
                    body.byteStream().use { inputStream ->
                        targetFile.outputStream().use { outputStream ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var totalBytesRead = 0L
                            val totalBytes = body.contentLength()
                            
                            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                                totalBytesRead += bytesRead
                                
                                // æ›´æ–°è¿›åº¦
                                if (totalBytes > 0) {
                                    val progress = ((totalBytesRead * 100) / totalBytes).toInt()
                                    onProgress(progress)
                                }
                            }
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
    
    // æ‰¹é‡ä¸‹è½½æ–‡ä»¶
    suspend fun downloadFiles(
        files: List<GadgetFile>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<File>> {
        return withContext(Dispatchers.IO) {
            try {
                val downloadedFiles = mutableListOf<File>()
                
                files.forEachIndexed { index, gadgetFile ->
                    _syncProgress.value = SyncProgress.Downloading(index + 1, files.size)
                    onProgress(index + 1, files.size)
                    
                    val result = downloadFile(gadgetFile) { progress ->
                        // å•ä¸ªæ–‡ä»¶ä¸‹è½½è¿›åº¦å¯ä»¥åœ¨è¿™é‡Œå¤„ç†
                    }
                    
                    if (result.isSuccess) {
                        downloadedFiles.add(result.getOrThrow())
                    } else {
                        Log.w("FileSyncManager", "Failed to download ${gadgetFile.filename}")
                    }
                }
                
                _syncProgress.value = SyncProgress.Success(downloadedFiles.size)
                Result.success(downloadedFiles)
            } catch (e: Exception) {
                _syncProgress.value = SyncProgress.Error(e.message ?: "Download error")
                Result.failure(e)
            }
        }
    }
    
    // æ™ºèƒ½åŒæ­¥ï¼šåªä¸‹è½½æ–°æ–‡ä»¶
    suspend fun smartSync(): Result<List<File>> {
        return withContext(Dispatchers.IO) {
            try {
                // èŽ·å–è¿œç¨‹æ–‡ä»¶åˆ—è¡¨
                val remoteFilesResult = syncFileList()
                if (remoteFilesResult.isFailure) {
                    return@withContext remoteFilesResult.map { emptyList<File>() }
                }
                
                val remoteFiles = remoteFilesResult.getOrThrow()
                
                // èŽ·å–æœ¬åœ°å·²æœ‰æ–‡ä»¶
                val localAudioFiles = audioDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
                val localImageFiles = imageDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
                
                // ç­›é€‰éœ€è¦ä¸‹è½½çš„æ–‡ä»¶
                val filesToDownload = remoteFiles.filter { file ->
                    when (file.type) {
                        "audio" -> !localAudioFiles.contains(file.filename)
                        "image" -> !localImageFiles.contains(file.filename)
                        else -> false
                    }
                }
                
                if (filesToDownload.isEmpty()) {
                    _syncProgress.value = SyncProgress.Success(0)
                    return@withContext Result.success(emptyList<File>())
                }
                
                // æ‰¹é‡ä¸‹è½½
                downloadFiles(filesToDownload) { current, total ->
                    Log.d("FileSyncManager", "Smart sync: $current/$total")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // èŽ·å–æœ¬åœ°æ–‡ä»¶
    fun getLocalAudioFiles(): List<File> {
        return audioDir.listFiles()?.toList() ?: emptyList()
    }
    
    fun getLocalImageFiles(): List<File> {
        return imageDir.listFiles()?.toList() ?: emptyList()
    }
    
    // æ¸…ç†æœ¬åœ°ç¼“å­˜
    suspend fun clearCache(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                audioDir.listFiles()?.forEach { it.delete() }
                imageDir.listFiles()?.forEach { it.delete() }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// åŒæ­¥è¿›åº¦çŠ¶æ€
sealed class SyncProgress {
    object Idle : SyncProgress()
    object Fetching : SyncProgress()
    data class Downloading(val current: Int, val total: Int) : SyncProgress()
    data class Success(val filesDownloaded: Int) : SyncProgress()
    data class Error(val message: String) : SyncProgress()
}
```

---

### 3.6.4 è®¾å¤‡æ˜¾ç¤ºæŽ§åˆ¶å™¨

```kotlin
class DeviceDisplayController(
    private val api: GadgetApi,
    private val deviceSettingDao: DeviceSettingDao
) {
    // å›¾ç‰‡çº¦æŸ
    companion object {
        const val MAX_IMAGE_SIZE = 50 * 1024      // 50KB
        const val IMAGE_WIDTH = 80
        const val IMAGE_HEIGHT = 80
        const val MAX_TEXT_LENGTH = 10
    }
    
    // ä¸Šä¼ å¹¶æ˜¾ç¤ºå›¾ç‰‡
    suspend fun uploadAndDisplayImage(imageFile: File): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // éªŒè¯å’Œå¤„ç†å›¾ç‰‡
                val processedImage = processImage(imageFile)
                if (processedImage.isFailure) {
                    return@withContext Result.failure(processedImage.exceptionOrNull()!!)
                }
                
                val validatedImage = processedImage.getOrThrow()
                
                // åˆ›å»ºmultipartè¯·æ±‚
                val requestFile = validatedImage.asRequestBody("image/png".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData(
                    "image",
                    validatedImage.name,
                    requestFile
                )
                
                // ä¸Šä¼ 
                val response = api.uploadImage(multipartBody)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val filename = response.body()!!.filename ?: validatedImage.name
                    
                    // æ›´æ–°æ•°æ®åº“
                    val device = deviceSettingDao.getCurrentDevice()
                    device?.let {
                        deviceSettingDao.updateCurrentImage(it.deviceId, filename)
                    }
                    
                    Result.success(filename)
                } else {
                    Result.failure(Exception("Upload failed: ${response.body()?.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // å¤„ç†å›¾ç‰‡ï¼ˆåŽ‹ç¼©ã€è°ƒæ•´å¤§å°ï¼‰
    private suspend fun processImage(imageFile: File): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                    ?: return@withContext Result.failure(Exception("Invalid image file"))
                
                // è°ƒæ•´å¤§å°åˆ°80x80
                val resizedBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    IMAGE_WIDTH,
                    IMAGE_HEIGHT,
                    true
                )
                
                // åŽ‹ç¼©åˆ°50KBä»¥ä¸‹
                val outputFile = File.createTempFile("gadget_image_", ".png")
                var quality = 100
                
                do {
                    outputFile.outputStream().use { out ->
                        resizedBitmap.compress(Bitmap.CompressFormat.PNG, quality, out)
                    }
                    quality -= 10
                } while (outputFile.length() > MAX_IMAGE_SIZE && quality > 0)
                
                if (outputFile.length() > MAX_IMAGE_SIZE) {
                    return@withContext Result.failure(
                        Exception("Unable to compress image to ${MAX_IMAGE_SIZE / 1024}KB")
                    )
                }
                
                bitmap.recycle()
                resizedBitmap.recycle()
                
                Result.success(outputFile)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // æ›´æ–°æ˜¾ç¤ºæ–‡å­—
    suspend fun updateDisplayText(text: String, duration: Int? = null): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // éªŒè¯æ–‡å­—é•¿åº¦
                if (text.length > MAX_TEXT_LENGTH) {
                    return@withContext Result.failure(
                        Exception("Text exceeds $MAX_TEXT_LENGTH characters")
                    )
                }
                
                val request = TextDisplayRequest(text, duration)
                val response = api.updateDisplayText(request)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    // æ›´æ–°æ•°æ®åº“
                    val device = deviceSettingDao.getCurrentDevice()
                    device?.let {
                        deviceSettingDao.updateCurrentText(it.deviceId, text)
                    }
                    
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to update text: ${response.body()?.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // èŽ·å–è®¾å¤‡çŠ¶æ€
    suspend fun getDeviceStatus(): Result<DeviceStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getDeviceStatus()
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(response.body()!!)
                } else {
                    Result.failure(Exception("Failed to get device status"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // åˆ é™¤è®¾å¤‡æ–‡ä»¶
    suspend fun deleteFile(filename: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.deleteFile(filename)
                
                if (response.isSuccessful && response.body()?.success == true) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to delete file"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

---

### 3.6.5 WiFiè¿žæŽ¥åŠ©æ‰‹

```kotlin
class WifiConnectionHelper(private val context: Context) {
    
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    
    // æ£€æŸ¥WiFiæ˜¯å¦å·²å¯ç”¨
    fun isWifiEnabled(): Boolean {
        return wifiManager.isWifiEnabled
    }
    
    // èŽ·å–å½“å‰è¿žæŽ¥çš„WiFi SSID
    fun getCurrentSsid(): String? {
        val wifiInfo = wifiManager.connectionInfo
        return wifiInfo?.ssid?.replace("\"", "")
    }
    
    // æ£€æŸ¥æ˜¯å¦è¿žæŽ¥åˆ°è®¾å¤‡WiFi
    fun isConnectedToGadget(deviceSsid: String): Boolean {
        val currentSsid = getCurrentSsid()
        return currentSsid == deviceSsid
    }
    
    // æ‰«æå¯ç”¨WiFiç½‘ç»œ
    @RequiresPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    fun scanWifiNetworks(onResult: (List<ScanResult>) -> Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val results = wifiManager.scanResults
                    onResult(results)
                    context.unregisterReceiver(this)
                }
            }
        }
        
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        
        wifiManager.startScan()
    }
    
    // è¿žæŽ¥åˆ°æŒ‡å®šWiFiï¼ˆAndroid 10+éœ€è¦ç”¨æˆ·æ‰‹åŠ¨æ“ä½œï¼‰
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToWifi(ssid: String, password: String) {
        val suggestion = WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .setIsAppInteractionRequired(true)
            .build()
        
        val suggestions = listOf(suggestion)
        val status = wifiManager.addNetworkSuggestions(suggestions)
        
        if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            Log.d("WifiHelper", "Network suggestion added successfully")
        } else {
            Log.e("WifiHelper", "Failed to add network suggestion: $status")
        }
    }
    
    // è¿žæŽ¥åˆ°WiFiï¼ˆAndroid 9åŠä»¥ä¸‹ - å·²å¼ƒç”¨ä½†ä»å¯ç”¨ï¼‰
    @Suppress("DEPRECATION")
    fun connectToWifiLegacy(ssid: String, password: String): Boolean {
        val wifiConfig = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            preSharedKey = "\"$password\""
        }
        
        val netId = wifiManager.addNetwork(wifiConfig)
        if (netId == -1) {
            return false
        }
        
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()
        
        return true
    }
    
    // æ£€æµ‹è®¾å¤‡æ˜¯å¦åœ¨çº¿ï¼ˆé€šè¿‡ping HTTPç«¯ç‚¹ï¼‰
    suspend fun isDeviceOnline(ipAddress: String, port: Int = 8080): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("http://$ipAddress:$port/api/device/status")
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                responseCode == 200
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // èŽ·å–è®¾å¤‡IPåœ°å€ï¼ˆå¦‚æžœåœ¨åŒä¸€ç½‘ç»œï¼‰
    suspend fun discoverDeviceIp(
        ssid: String,
        portToScan: Int = 8080
    ): String? {
        return withContext(Dispatchers.IO) {
            // æ£€æŸ¥æ˜¯å¦è¿žæŽ¥åˆ°æ­£ç¡®çš„WiFi
            if (getCurrentSsid() != ssid) {
                return@withContext null
            }
            
            // èŽ·å–æœ¬æœºIP
            val wifiInfo = wifiManager.connectionInfo
            val localIp = wifiInfo.ipAddress
            
            // è½¬æ¢ä¸ºç½‘æ®µ
            val ipString = String.format(
                "%d.%d.%d",
                localIp and 0xff,
                localIp shr 8 and 0xff,
                localIp shr 16 and 0xff
            )
            
            // æ‰«æ192.168.x.1-254
            for (i in 1..254) {
                val testIp = "$ipString.$i"
                if (isDeviceOnline(testIp, portToScan)) {
                    return@withContext testIp
                }
            }
            
            null
        }
    }
}
```

---

### 3.6.6 æ–‡ä»¶åŒæ­¥ä»“åº“ï¼ˆæ•´åˆï¼‰

```kotlin
class FileSyncRepository(
    private val fileSyncManager: FileSyncManager,
    private val deviceDisplayController: DeviceDisplayController,
    private val wifiHelper: WifiConnectionHelper,
    private val deviceSettingDao: DeviceSettingDao
) {
    
    // åˆå§‹åŒ–è¿žæŽ¥
    suspend fun initializeConnection(deviceIp: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // é…ç½®APIå®¢æˆ·ç«¯
                GadgetApiClient.configure(deviceIp)
                
                // æµ‹è¯•è¿žæŽ¥
                val isOnline = wifiHelper.isDeviceOnline(deviceIp)
                if (!isOnline) {
                    return@withContext Result.failure(Exception("Device is not reachable"))
                }
                
                // èŽ·å–è®¾å¤‡çŠ¶æ€
                val statusResult = deviceDisplayController.getDeviceStatus()
                if (statusResult.isSuccess) {
                    val status = statusResult.getOrThrow()
                    
                    // æ›´æ–°è®¾å¤‡ä¿¡æ¯
                    deviceSettingDao.updateLastConnected(
                        status.deviceId,
                        System.currentTimeMillis()
                    )
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // å®Œæ•´åŒæ­¥æµç¨‹
    suspend fun performFullSync(): Result<SyncResult> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. æ™ºèƒ½åŒæ­¥æ–‡ä»¶
                val syncResult = fileSyncManager.smartSync()
                if (syncResult.isFailure) {
                    return@withContext Result.failure(syncResult.exceptionOrNull()!!)
                }
                
                val downloadedFiles = syncResult.getOrThrow()
                
                // 2. èŽ·å–è®¾å¤‡çŠ¶æ€
                val statusResult = deviceDisplayController.getDeviceStatus()
                val deviceStatus = if (statusResult.isSuccess) {
                    statusResult.getOrThrow()
                } else null
                
                Result.success(
                    SyncResult(
                        filesDownloaded = downloadedFiles.size,
                        audioFiles = downloadedFiles.count { it.extension in listOf("mp3", "wav", "m4a") },
                        imageFiles = downloadedFiles.count { it.extension in listOf("gif", "png", "jpg") },
                        deviceStatus = deviceStatus
                    )
                )
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // è‡ªåŠ¨å‘çŽ°å¹¶è¿žæŽ¥è®¾å¤‡
    suspend fun autoDiscoverAndConnect(ssid: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // æ£€æŸ¥WiFiè¿žæŽ¥
                if (wifiHelper.getCurrentSsid() != ssid) {
                    return@withContext Result.failure(
                        Exception("Not connected to $ssid")
                    )
                }
                
                // å‘çŽ°è®¾å¤‡IP
                val deviceIp = wifiHelper.discoverDeviceIp(ssid)
                    ?: return@withContext Result.failure(
                        Exception("Device not found on network")
                    )
                
                // åˆå§‹åŒ–è¿žæŽ¥
                val initResult = initializeConnection(deviceIp)
                if (initResult.isFailure) {
                    return@withContext initResult.map { deviceIp }
                }
                
                Result.success(deviceIp)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

data class SyncResult(
    val filesDownloaded: Int,
    val audioFiles: Int,
    val imageFiles: Int,
    val deviceStatus: DeviceStatusResponse?
)
```

---

### 3.6.7 ä½¿ç”¨ç¤ºä¾‹

```kotlin
class SyncViewModel(
    private val fileSyncRepository: FileSyncRepository
) : ViewModel() {
    
    private val _syncState = MutableStateFlow<UiState<SyncResult>>(UiState.Idle)
    val syncState: StateFlow<UiState<SyncResult>> = _syncState.asStateFlow()
    
    // æ‰§è¡ŒåŒæ­¥
    fun performSync(deviceIp: String) {
        viewModelScope.launch {
            _syncState.value = UiState.Loading
            
            // åˆå§‹åŒ–è¿žæŽ¥
            val initResult = fileSyncRepository.initializeConnection(deviceIp)
            if (initResult.isFailure) {
                _syncState.value = UiState.Error(initResult.exceptionOrNull()?.message ?: "Connection failed")
                return@launch
            }
            
            // æ‰§è¡ŒåŒæ­¥
            val syncResult = fileSyncRepository.performFullSync()
            _syncState.value = when {
                syncResult.isSuccess -> UiState.Success(syncResult.getOrThrow())
                else -> UiState.Error(syncResult.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }
    
    // è‡ªåŠ¨å‘çŽ°å¹¶åŒæ­¥
    fun autoSync(ssid: String) {
        viewModelScope.launch {
            _syncState.value = UiState.Loading
            
            val discoverResult = fileSyncRepository.autoDiscoverAndConnect(ssid)
            if (discoverResult.isFailure) {
                _syncState.value = UiState.Error("Device not found")
                return@launch
            }
            
            val syncResult = fileSyncRepository.performFullSync()
            _syncState.value = when {
                syncResult.isSuccess -> UiState.Success(syncResult.getOrThrow())
                else -> UiState.Error("Sync failed")
            }
        }
    }
}

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

---

## æ€»ç»“

âœ… **WiFiæ–‡ä»¶åŒæ­¥æ¨¡å—å·²å®Œæˆ**ï¼ŒåŒ…æ‹¬ï¼š

1. **HTTP APIå®šä¹‰** - RetrofitæŽ¥å£å’Œæ•°æ®æ¨¡åž‹
2. **æ–‡ä»¶åŒæ­¥ç®¡ç†å™¨** - ä¸‹è½½ã€æ™ºèƒ½åŒæ­¥ã€è¿›åº¦è·Ÿè¸ª
3. **è®¾å¤‡æ˜¾ç¤ºæŽ§åˆ¶** - å›¾ç‰‡ä¸Šä¼ ï¼ˆ80x80, 50KBé™åˆ¶ï¼‰ã€æ–‡å­—æ˜¾ç¤ºï¼ˆ10å­—ç¬¦ï¼‰
4. **WiFiè¿žæŽ¥åŠ©æ‰‹** - ç½‘ç»œæ£€æµ‹ã€è®¾å¤‡å‘çŽ°ã€IPæ‰«æ
5. **Repositoryå±‚** - ç»Ÿä¸€ç®¡ç†åŒæ­¥æµç¨‹
6. **ä½¿ç”¨ç¤ºä¾‹** - ViewModelé›†æˆ

## ä¸‹ä¸€æ­¥é€‰æ‹©ï¼š

2. **AIé›†æˆæ¨¡å—** â†’ Qwen Dashscope (èŠå¤©) + Tingwu (è½¬å†™/åˆ†ç¦»/æ—¶é—´æˆ³)
3. **PDF/CSVå¯¼å‡ºæ¨¡å—** â†’ ç”ŸæˆCRMæ ¼å¼æ–‡æ¡£
4. **UIå±‚ (Jetpack Compose)** â†’ èŠå¤©ç•Œé¢ã€åŽ†å²è®°å½•ã€è®¾å¤‡é…å¯¹

ç»§ç»­å“ªä¸ªæ¨¡å—ï¼Ÿ

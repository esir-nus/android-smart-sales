# Smart Sales Assistant - Data Layer (Continued)

## 3.4 æ•°æ®æŒä¹…åŒ–æ¨¡å— (ç»­)

### 3.4.1 æ•°æ®åº“è¡¨è®¾è®¡ (å®Œæ•´ç‰ˆ)

```kotlin
// å¯¹è¯è¡¨
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,              // å¯¹è¯æ ‡é¢˜
    val summary: String?,           // æ‘˜è¦
    val createdAt: Long,            // åˆ›å»ºæ—¶é—´æˆ³
    val updatedAt: Long,            // æ›´æ–°æ—¶é—´æˆ³
    val isAnalyzed: Boolean = false // æ˜¯å¦å·²åˆ†æžå®¢æˆ·
)

// æ¶ˆæ¯è¡¨
@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val role: String,               // "user" | "assistant"
    val content: String,            // æ–‡æœ¬å†…å®¹
    val timestamp: Long,
    val attachments: String?,       // JSONå­—ç¬¦ä¸²: [{"type":"audio","path":"..."},{"type":"image","path":"..."}]
    val metadata: String?           // JSONå­—ç¬¦ä¸²: {"tokens":1234,"model":"qwen-max"}
)

// é™„ä»¶è¡¨
@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val type: String,               // "audio" | "image" | "document"
    val filePath: String,           // æœ¬åœ°æ–‡ä»¶è·¯å¾„
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedAt: Long,
    val transcription: String?      // ä»…éŸ³é¢‘æ–‡ä»¶æœ‰è½¬å†™å†…å®¹
)

// WiFié…ç½®è¡¨
@Entity(tableName = "wifi_configs")
data class WifiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val password: String,           // åŠ å¯†å­˜å‚¨
    val isDefault: Boolean = false,
    val lastUsed: Long,
    val addedAt: Long
)

// è®¾å¤‡é…ç½®è¡¨
@Entity(tableName = "device_settings")
data class DeviceSettingEntity(
    @PrimaryKey
    val deviceId: String,           // BLE MACåœ°å€
    val deviceName: String,
    val lastConnected: Long,
    val currentImage: String?,      // å½“å‰æ˜¾ç¤ºçš„å›¾ç‰‡æ–‡ä»¶å
    val currentText: String?,       // å½“å‰æ˜¾ç¤ºçš„æ–‡å­—
    val isPaired: Boolean = true
)

// CRMå¯¼å‡ºè®°å½•è¡¨
@Entity(tableName = "crm_exports")
data class CrmExportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val exportType: String,         // "csv" | "pdf"
    val filePath: String,
    val exportedAt: Long,
    val status: String              // "success" | "failed"
)
```

---

### 3.4.2 DAOæŽ¥å£

```kotlin
// å¯¹è¯DAO
@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: Long): ConversationEntity?
    
    @Query("""
        SELECT * FROM conversations 
        WHERE createdAt >= :startTime AND createdAt < :endTime 
        ORDER BY createdAt DESC
    """)
    fun getConversationsByTimeRange(startTime: Long, endTime: Long): Flow<List<ConversationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)
    
    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)
    
    @Query("UPDATE conversations SET isAnalyzed = :analyzed WHERE id = :id")
    suspend fun markAsAnalyzed(id: Long, analyzed: Boolean)
}

// æ¶ˆæ¯DAO
@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: Long): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: Long): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesByConversation(conversationId: Long)
    
    @Query("""
        SELECT m.* FROM messages m 
        INNER JOIN conversations c ON m.conversationId = c.id 
        WHERE m.attachments IS NOT NULL 
        ORDER BY m.timestamp DESC 
        LIMIT :limit
    """)
    suspend fun getRecentMessagesWithAttachments(limit: Int = 20): List<MessageEntity>
}

// é™„ä»¶DAO
@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun getAttachmentsByMessage(messageId: Long): List<AttachmentEntity>
    
    @Query("SELECT * FROM attachments WHERE type = :type ORDER BY uploadedAt DESC LIMIT :limit")
    suspend fun getAttachmentsByType(type: String, limit: Int = 50): List<AttachmentEntity>
    
    @Insert
    suspend fun insertAttachment(attachment: AttachmentEntity): Long
    
    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)
    
    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsByMessage(messageId: Long)
}

// WiFié…ç½®DAO
@Dao
interface WifiConfigDao {
    @Query("SELECT * FROM wifi_configs ORDER BY lastUsed DESC")
    fun getAllWifiConfigs(): Flow<List<WifiConfigEntity>>
    
    @Query("SELECT * FROM wifi_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWifiConfig(): WifiConfigEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiConfig(config: WifiConfigEntity): Long
    
    @Update
    suspend fun updateWifiConfig(config: WifiConfigEntity)
    
    @Delete
    suspend fun deleteWifiConfig(config: WifiConfigEntity)
    
    @Query("UPDATE wifi_configs SET isDefault = 0")
    suspend fun clearAllDefaults()
    
    @Query("UPDATE wifi_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: Long)
}

// è®¾å¤‡é…ç½®DAO
@Dao
interface DeviceSettingDao {
    @Query("SELECT * FROM device_settings WHERE isPaired = 1 LIMIT 1")
    suspend fun getCurrentDevice(): DeviceSettingEntity?
    
    @Query("SELECT * FROM device_settings ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<DeviceSettingEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceSettingEntity)
    
    @Update
    suspend fun updateDevice(device: DeviceSettingEntity)
    
    @Query("UPDATE device_settings SET lastConnected = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastConnected(deviceId: String, timestamp: Long)
    
    @Query("UPDATE device_settings SET currentImage = :imageName WHERE deviceId = :deviceId")
    suspend fun updateCurrentImage(deviceId: String, imageName: String?)
    
    @Query("UPDATE device_settings SET currentText = :text WHERE deviceId = :deviceId")
    suspend fun updateCurrentText(deviceId: String, text: String?)
}

// CRMå¯¼å‡ºDAO
@Dao
interface CrmExportDao {
    @Query("SELECT * FROM crm_exports ORDER BY exportedAt DESC LIMIT :limit")
    suspend fun getRecentExports(limit: Int = 50): List<CrmExportEntity>
    
    @Query("SELECT * FROM crm_exports WHERE conversationId = :conversationId")
    suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity>
    
    @Insert
    suspend fun insertExport(export: CrmExportEntity): Long
    
    @Delete
    suspend fun deleteExport(export: CrmExportEntity)
}
```

---

### 3.4.3 Roomæ•°æ®åº“é…ç½®

```kotlin
@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        AttachmentEntity::class,
        WifiConfigEntity::class,
        DeviceSettingEntity::class,
        CrmExportEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class SmartSalesDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun wifiConfigDao(): WifiConfigDao
    abstract fun deviceSettingDao(): DeviceSettingDao
    abstract fun crmExportDao(): CrmExportDao
    
    companion object {
        @Volatile
        private var INSTANCE: SmartSalesDatabase? = null
        
        fun getDatabase(context: Context): SmartSalesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartSalesDatabase::class.java,
                    "smart_sales_database"
                )
                    .addMigrations() // æ·»åŠ è¿ç§»ç­–ç•¥
                    .fallbackToDestructiveMigration() // å¼€å‘é˜¶æ®µä½¿ç”¨
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

### 3.4.4 Repositoryå±‚

```kotlin
// å¯¹è¯ä»“åº“
class ConversationRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao
) {
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    
    suspend fun getConversationWithMessages(conversationId: Long): ConversationWithMessages? {
        val conversation = conversationDao.getConversationById(conversationId) ?: return null
        val messages = messageDao.getMessagesByConversation(conversationId).first()
        return ConversationWithMessages(conversation, messages)
    }
    
    suspend fun createConversation(title: String): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insertConversation(
            ConversationEntity(
                title = title,
                createdAt = now,
                updatedAt = now
            )
        )
    }
    
    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String,
        attachments: List<Attachment>? = null
    ): Long {
        // æ›´æ–°å¯¹è¯æ—¶é—´æˆ³
        conversationDao.getConversationById(conversationId)?.let {
            conversationDao.updateConversation(it.copy(updatedAt = System.currentTimeMillis()))
        }
        
        // æ’å…¥æ¶ˆæ¯
        val messageId = messageDao.insertMessage(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
                attachments = attachments?.let { Json.encodeToString(it) }
            )
        )
        
        // æ’å…¥é™„ä»¶
        attachments?.forEach { attachment ->
            attachmentDao.insertAttachment(
                AttachmentEntity(
                    messageId = messageId,
                    type = attachment.type,
                    filePath = attachment.path,
                    fileName = attachment.name,
                    fileSize = attachment.size,
                    mimeType = attachment.mimeType,
                    uploadedAt = System.currentTimeMillis()
                )
            )
        }
        
        return messageId
    }
    
    suspend fun deleteConversation(conversationId: Long) {
        // çº§è”åˆ é™¤ä¼šè‡ªåŠ¨åˆ é™¤ç›¸å…³æ¶ˆæ¯å’Œé™„ä»¶
        conversationDao.deleteConversationById(conversationId)
    }
    
    suspend fun getConversationsGroupedByTime(): Map<String, List<ConversationEntity>> {
        val conversations = conversationDao.getAllConversations().first()
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60 * 1000L
        val thirtyDaysAgo = now - 30 * 24 * 60 * 60 * 1000L
        
        return conversations.groupBy { conversation ->
            when {
                conversation.createdAt >= sevenDaysAgo -> "7å¤©å†…"
                conversation.createdAt >= thirtyDaysAgo -> "30å¤©å†…"
                else -> {
                    val date = Date(conversation.createdAt)
                    SimpleDateFormat("yyyyå¹´MMæœˆ", Locale.CHINESE).format(date)
                }
            }
        }
    }
}

// è®¾å¤‡ä»“åº“
class DeviceRepository(
    private val deviceSettingDao: DeviceSettingDao,
    private val wifiConfigDao: WifiConfigDao
) {
    suspend fun getCurrentDevice(): DeviceSettingEntity? {
        return deviceSettingDao.getCurrentDevice()
    }
    
    suspend fun saveDevice(
        deviceId: String,
        deviceName: String
    ) {
        deviceSettingDao.insertDevice(
            DeviceSettingEntity(
                deviceId = deviceId,
                deviceName = deviceName,
                lastConnected = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun updateDeviceConnection(deviceId: String) {
        deviceSettingDao.updateLastConnected(deviceId, System.currentTimeMillis())
    }
    
    suspend fun saveWifiConfig(ssid: String, password: String, isDefault: Boolean = false) {
        if (isDefault) {
            wifiConfigDao.clearAllDefaults()
        }
        wifiConfigDao.insertWifiConfig(
            WifiConfigEntity(
                ssid = ssid,
                password = password,
                isDefault = isDefault,
                lastUsed = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis()
            )
        )
    }
    
    suspend fun getDefaultWifi(): WifiConfigEntity? {
        return wifiConfigDao.getDefaultWifiConfig()
    }
    
    val allWifiConfigs: Flow<List<WifiConfigEntity>> = wifiConfigDao.getAllWifiConfigs()
}

// å¯¼å‡ºä»“åº“
class ExportRepository(
    private val crmExportDao: CrmExportDao
) {
    suspend fun recordExport(
        conversationId: Long,
        exportType: String,
        filePath: String,
        status: String
    ): Long {
        return crmExportDao.insertExport(
            CrmExportEntity(
                conversationId = conversationId,
                exportType = exportType,
                filePath = filePath,
                exportedAt = System.currentTimeMillis(),
                status = status
            )
        )
    }
    
    suspend fun getRecentExports(limit: Int = 50): List<CrmExportEntity> {
        return crmExportDao.getRecentExports(limit)
    }
}

// æ•°æ®æ¨¡åž‹
data class ConversationWithMessages(
    val conversation: ConversationEntity,
    val messages: List<MessageEntity>
)

data class Attachment(
    val type: String,
    val path: String,
    val name: String,
    val size: Long,
    val mimeType: String
)
```

---

## 3.5 BLEè¿žæŽ¥æ¨¡å—

### 3.5.1 BLEæœåŠ¡å®šä¹‰

```kotlin
// BLEæœåŠ¡å’Œç‰¹å¾UUID
object BleConstants {
    // è‡ªå®šä¹‰æœåŠ¡UUID
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    
    // WiFié…ç½®ç‰¹å¾ï¼ˆå†™å…¥SSIDå’Œå¯†ç ï¼‰
    val CHAR_WIFI_CONFIG: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    
    // è®¾å¤‡çŠ¶æ€ç‰¹å¾ï¼ˆè¯»å–è®¾å¤‡ä¿¡æ¯ï¼‰
    val CHAR_DEVICE_STATUS: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    
    // å‘½ä»¤ç‰¹å¾ï¼ˆå‘é€æŽ§åˆ¶å‘½ä»¤ï¼‰
    val CHAR_COMMAND: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    
    // WiFié…ç½®åè®®
    // æ ¼å¼: SSID|PASSWORD (ä½¿ç”¨|åˆ†éš”ç¬¦ï¼ŒUTF-8ç¼–ç )
    fun encodeWifiConfig(ssid: String, password: String): ByteArray {
        return "$ssid|$password".toByteArray(Charsets.UTF_8)
    }
    
    fun decodeWifiConfig(data: ByteArray): Pair<String, String>? {
        val config = String(data, Charsets.UTF_8)
        val parts = config.split("|")
        return if (parts.size == 2) Pair(parts[0], parts[1]) else null
    }
}

// BLEå‘½ä»¤ç±»åž‹
enum class BleCommand(val value: Byte) {
    SYNC_FILES(0x01),           // åŒæ­¥æ–‡ä»¶åˆ—è¡¨
    UPDATE_IMAGE(0x02),         // æ›´æ–°æ˜¾ç¤ºå›¾ç‰‡
    UPDATE_TEXT(0x03),          // æ›´æ–°æ˜¾ç¤ºæ–‡å­—
    GET_DEVICE_INFO(0x04),      // èŽ·å–è®¾å¤‡ä¿¡æ¯
    REBOOT(0x05);               // é‡å¯è®¾å¤‡
    
    fun toByteArray(): ByteArray = byteArrayOf(value)
}
```

---

### 3.5.2 BLEç®¡ç†å™¨

```kotlin
@SuppressLint("MissingPermission") // å‡è®¾æƒé™å·²åœ¨è°ƒç”¨å‰æ£€æŸ¥
class BleManager(private val context: Context) {
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }
    
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    
    private var bluetoothGatt: BluetoothGatt? = null
    
    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    // æ‰«æå›žè°ƒ
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val currentDevices = _discoveredDevices.value.toMutableList()
            if (!currentDevices.any { it.address == device.address }) {
                currentDevices.add(device)
                _discoveredDevices.value = currentDevices
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e("BleManager", "Scan failed with error: $errorCode")
        }
    }
    
    // GATTå›žè°ƒ
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = BleConnectionState.Connected(gatt.device)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = BleConnectionState.Disconnected
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.ServicesDiscovered(gatt.device)
            }
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BleManager", "Characteristic write successful: ${characteristic.uuid}")
            } else {
                Log.e("BleManager", "Characteristic write failed: $status")
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                Log.d("BleManager", "Characteristic read: ${data.contentToString()}")
            }
        }
    }
    
    // å¼€å§‹æ‰«æ
    fun startScan() {
        _discoveredDevices.value = emptyList()
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }
    
    // åœæ­¢æ‰«æ
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }
    
    // è¿žæŽ¥è®¾å¤‡
    fun connectDevice(device: BluetoothDevice) {
        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    // æ–­å¼€è¿žæŽ¥
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
    
    // å‘é€WiFié…ç½®
    suspend fun sendWifiConfig(ssid: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    Exception("Not connected to device")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_WIFI_CONFIG)
                
                if (characteristic != null) {
                    val data = BleConstants.encodeWifiConfig(ssid, password)
                    characteristic.value = data
                    val success = gatt.writeCharacteristic(characteristic)
                    
                    if (success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to write WiFi config"))
                    }
                } else {
                    Result.failure(Exception("WiFi config characteristic not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // å‘é€å‘½ä»¤
    suspend fun sendCommand(command: BleCommand): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    Exception("Not connected to device")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_COMMAND)
                
                if (characteristic != null) {
                    characteristic.value = command.toByteArray()
                    val success = gatt.writeCharacteristic(characteristic)
                    
                    if (success) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to send command"))
                    }
                } else {
                    Result.failure(Exception("Command characteristic not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    // è¯»å–è®¾å¤‡çŠ¶æ€
    suspend fun readDeviceStatus(): Result<ByteArray> {
        return withContext(Dispatchers.IO) {
            try {
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    Exception("Not connected to device")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_DEVICE_STATUS)
                
                if (characteristic != null) {
                    val success = gatt.readCharacteristic(characteristic)
                    if (success) {
                        delay(500) // ç­‰å¾…è¯»å–å®Œæˆ
                        Result.success(characteristic.value)
                    } else {
                        Result.failure(Exception("Failed to read device status"))
                    }
                } else {
                    Result.failure(Exception("Device status characteristic not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

// BLEè¿žæŽ¥çŠ¶æ€
sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    data class Connected(val device: BluetoothDevice) : BleConnectionState()
    data class ServicesDiscovered(val device: BluetoothDevice) : BleConnectionState()
}
```

---

## ä¸‹ä¸€æ­¥è®¡åˆ’

æ•°æ®å±‚å’ŒBLEè¿žæŽ¥æ¨¡å—å·²å®Œæˆã€‚æŽ¥ä¸‹æ¥åº”è¯¥ï¼š

1. **WiFiæ–‡ä»¶åŒæ­¥æ¨¡å—** - HTTPå®¢æˆ·ç«¯è¿žæŽ¥ç¡¬ä»¶è®¾å¤‡çš„æœåŠ¡å™¨
2. **AIé›†æˆæ¨¡å—** - Qwen APIè°ƒç”¨ï¼ˆDashscopeèŠå¤© + Tingwuè½¬å†™ï¼‰
3. **PDF/CSVå¯¼å‡ºæ¨¡å—** - ç”Ÿæˆæ ¼å¼åŒ–æ–‡æ¡£
4. **UIå±‚** - Jetpack Composeç•Œé¢å®žçŽ°

æ‚¨å¸Œæœ›ç»§ç»­å“ªä¸ªæ¨¡å—ï¼Ÿ

# Smart Sales Assistant - Data Layer

## Overview

This document provides the complete data layer implementation including:
- Database schema and entities
- DAO (Data Access Object) interfaces  
- Repository pattern
- BLE (Bluetooth Low Energy) communication

---

## 1. Database Entities

### 1.1 Conversation Entity

```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,              // Conversation title
    val summary: String?,           // Summary
    val createdAt: Long,            // Creation timestamp
    val updatedAt: Long,            // Update timestamp
    val isAnalyzed: Boolean = false // Whether customer analysis is complete
)
```

### 1.2 Message Entity

```kotlin
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
    val content: String,            // Text content
    val timestamp: Long,
    val attachments: String?,       // JSON string: [{"type":"audio","path":"..."}]
    val metadata: String?           // JSON string: {"tokens":1234,"model":"qwen-max"}
)
```

### 1.3 Attachment Entity

```kotlin
@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long,
    val type: String,               // "audio" | "image" | "document"
    val filePath: String,           // Local file path
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedAt: Long,
    val transcription: String?      // Audio transcription (audio files only)
)
```

### 1.4 WiFi Configuration Entity

```kotlin
@Entity(tableName = "wifi_configs")
data class WifiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val password: String,           // Encrypted storage
    val isDefault: Boolean = false,
    val lastUsed: Long,
    val addedAt: Long
)
```

### 1.5 Device Settings Entity

```kotlin
@Entity(tableName = "device_settings")
data class DeviceSettingEntity(
    @PrimaryKey
    val deviceId: String,           // BLE MAC address
    val deviceName: String,
    val lastConnected: Long,
    val currentImage: String?,      // Current display image filename
    val currentText: String?,       // Current display text
    val isPaired: Boolean = true
)
```

### 1.6 CRM Export Record Entity

```kotlin
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

## 2. DAO Interfaces

### 2.1 ConversationDao

```kotlin
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
```

### 2.2 MessageDao

```kotlin
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
```

### 2.3 AttachmentDao

```kotlin
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
```

### 2.4 WifiConfigDao

```kotlin
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
```

### 2.5 DeviceSettingDao

```kotlin
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
```

### 2.6 CrmExportDao

```kotlin
@Dao
interface CrmExportDao {
    @Query("SELECT * FROM crm_exports ORDER BY exportedAt DESC")
    fun getAllExports(): Flow<List<CrmExportEntity>>
    
    @Query("SELECT * FROM crm_exports WHERE conversationId = :conversationId")
    suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity>
    
    @Insert
    suspend fun insertExport(export: CrmExportEntity): Long
    
    @Delete
    suspend fun deleteExport(export: CrmExportEntity)
}
```

---

## 3. Database Configuration

### 3.1 Database Class

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
}
```

### 3.2 Database Module (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartSalesDatabase {
        return Room.databaseBuilder(
            context,
            SmartSalesDatabase::class.java,
            "smart_sales.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideConversationDao(database: SmartSalesDatabase): ConversationDao {
        return database.conversationDao()
    }
    
    @Provides
    fun provideMessageDao(database: SmartSalesDatabase): MessageDao {
        return database.messageDao()
    }
    
    @Provides
    fun provideAttachmentDao(database: SmartSalesDatabase): AttachmentDao {
        return database.attachmentDao()
    }
    
    @Provides
    fun provideWifiConfigDao(database: SmartSalesDatabase): WifiConfigDao {
        return database.wifiConfigDao()
    }
    
    @Provides
    fun provideDeviceSettingDao(database: SmartSalesDatabase): DeviceSettingDao {
        return database.deviceSettingDao()
    }
    
    @Provides
    fun provideCrmExportDao(database: SmartSalesDatabase): CrmExportDao {
        return database.crmExportDao()
    }
}
```

---

## 4. Repository Layer

### 4.1 ConversationRepository

```kotlin
class ConversationRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    // Get all conversations as Flow
    fun getAllConversations(): Flow<List<ConversationEntity>> {
        return conversationDao.getAllConversations()
    }
    
    // Get conversation by ID
    suspend fun getConversationById(id: Long): ConversationEntity? {
        return conversationDao.getConversationById(id)
    }
    
    // Create new conversation
    suspend fun createConversation(title: String): Result<Long> {
        return try {
            val conversation = ConversationEntity(
                title = title,
                summary = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val id = conversationDao.insertConversation(conversation)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Add message to conversation
    suspend fun addMessage(
        conversationId: Long,
        role: String,
        content: String
    ): Result<Long> {
        return try {
            val message = MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                timestamp = System.currentTimeMillis(),
                attachments = null,
                metadata = null
            )
            val messageId = messageDao.insertMessage(message)
            
            // Update conversation timestamp
            conversationDao.getConversationById(conversationId)?.let { conv ->
                conversationDao.updateConversation(
                    conv.copy(updatedAt = System.currentTimeMillis())
                )
            }
            
            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get messages for conversation
    fun getMessages(conversationId: Long): Flow<List<MessageEntity>> {
        return messageDao.getMessagesByConversation(conversationId)
    }
    
    // Delete conversation
    suspend fun deleteConversation(id: Long): Result<Unit> {
        return try {
            conversationDao.deleteConversationById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Mark conversation as analyzed
    suspend fun markAsAnalyzed(id: Long): Result<Unit> {
        return try {
            conversationDao.markAsAnalyzed(id, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 4.2 DeviceRepository

```kotlin
class DeviceRepository @Inject constructor(
    private val deviceSettingDao: DeviceSettingDao,
    private val wifiConfigDao: WifiConfigDao
) {
    // Get current paired device
    suspend fun getCurrentDevice(): DeviceSettingEntity? {
        return deviceSettingDao.getCurrentDevice()
    }
    
    // Save device settings
    suspend fun saveDevice(device: DeviceSettingEntity): Result<Unit> {
        return try {
            deviceSettingDao.insertDevice(device)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update last connected timestamp
    suspend fun updateLastConnected(deviceId: String): Result<Unit> {
        return try {
            deviceSettingDao.updateLastConnected(
                deviceId,
                System.currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // WiFi configuration methods
    fun getAllWifiConfigs(): Flow<List<WifiConfigEntity>> {
        return wifiConfigDao.getAllWifiConfigs()
    }
    
    suspend fun getDefaultWifiConfig(): WifiConfigEntity? {
        return wifiConfigDao.getDefaultWifiConfig()
    }
    
    suspend fun saveWifiConfig(ssid: String, password: String): Result<Long> {
        return try {
            val config = WifiConfigEntity(
                ssid = ssid,
                password = password,
                isDefault = false,
                lastUsed = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis()
            )
            val id = wifiConfigDao.insertWifiConfig(config)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun setDefaultWifiConfig(id: Long): Result<Unit> {
        return try {
            wifiConfigDao.clearAllDefaults()
            wifiConfigDao.setAsDefault(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 4.3 ExportRepository

```kotlin
class ExportRepository @Inject constructor(
    private val crmExportDao: CrmExportDao
) {
    // Get all export records
    fun getAllExports(): Flow<List<CrmExportEntity>> {
        return crmExportDao.getAllExports()
    }
    
    // Get exports for a conversation
    suspend fun getExportsByConversation(conversationId: Long): List<CrmExportEntity> {
        return crmExportDao.getExportsByConversation(conversationId)
    }
    
    // Save export record
    suspend fun saveExport(
        conversationId: Long,
        exportType: String,
        filePath: String,
        status: String
    ): Result<Long> {
        return try {
            val export = CrmExportEntity(
                conversationId = conversationId,
                exportType = exportType,
                filePath = filePath,
                exportedAt = System.currentTimeMillis(),
                status = status
            )
            val id = crmExportDao.insertExport(export)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete export record
    suspend fun deleteExport(export: CrmExportEntity): Result<Unit> {
        return try {
            crmExportDao.deleteExport(export)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

### 4.4 Repository Module (Hilt)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao
    ): ConversationRepository {
        return ConversationRepository(conversationDao, messageDao)
    }
    
    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceSettingDao: DeviceSettingDao,
        wifiConfigDao: WifiConfigDao
    ): DeviceRepository {
        return DeviceRepository(deviceSettingDao, wifiConfigDao)
    }
    
    @Provides
    @Singleton
    fun provideExportRepository(
        crmExportDao: CrmExportDao
    ): ExportRepository {
        return ExportRepository(crmExportDao)
    }
}
```

---

## 5. BLE (Bluetooth Low Energy) Communication

### 5.1 BLE Constants

```kotlin
object BleConstants {
    // UUIDs (Replace with your actual hardware UUIDs)
    val SERVICE_UUID: UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
    val CHAR_WIFI_CONFIG: UUID = UUID.fromString("00001235-0000-1000-8000-00805f9b34fb")
    val CHAR_COMMAND: UUID = UUID.fromString("00001236-0000-1000-8000-00805f9b34fb")
    val CHAR_DEVICE_STATUS: UUID = UUID.fromString("00001237-0000-1000-8000-00805f9b34fb")
    
    // WiFi config packet format
    fun encodeWifiConfig(ssid: String, password: String): ByteArray {
        val ssidBytes = ssid.toByteArray(Charsets.UTF_8)
        val passwordBytes = password.toByteArray(Charsets.UTF_8)
        
        return ByteBuffer.allocate(2 + ssidBytes.size + passwordBytes.size)
            .put(ssidBytes.size.toByte())
            .put(ssidBytes)
            .put(passwordBytes.size.toByte())
            .put(passwordBytes)
            .array()
    }
}

// BLE Command enum
enum class BleCommand(val value: Byte) {
    START_RECORDING(0x01),
    STOP_RECORDING(0x02),
    SHOW_IMAGE(0x03),
    SHOW_TEXT(0x04),
    CLEAR_DISPLAY(0x05);
    
    fun toByteArray(): ByteArray = byteArrayOf(value)
}
```

### 5.2 BLE Manager

```kotlin
@SuppressLint("MissingPermission") // Assume permissions checked before calling
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
    
    // Scan callback
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
    
    // GATT callback
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
    
    // Start scanning
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
    
    // Stop scanning
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }
    
    // Connect to device
    fun connectDevice(device: BluetoothDevice) {
        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    // Disconnect
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
    
    // Send WiFi configuration
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
    
    // Send command
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
    
    // Read device status
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
                        delay(500) // Wait for read completion
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

// BLE connection state
sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    data class Connected(val device: BluetoothDevice) : BleConnectionState()
    data class ServicesDiscovered(val device: BluetoothDevice) : BleConnectionState()
}
```

---

## Summary

This data layer implementation provides:

✅ **Complete database schema** with 6 entities  
✅ **6 DAO interfaces** for data access  
✅ **3 repository classes** with business logic  
✅ **BLE communication** for hardware integration  
✅ **Hilt dependency injection** modules  
✅ **Flow-based reactive data** for UI updates  

### Next Steps

1. **WiFi File Sync Module** - HTTP client for device communication
2. **AI Integration Module** - Qwen API (Dashscope + Tingwu)
3. **Export System Module** - PDF/CSV generation
4. **UI Layer** - Jetpack Compose screens

Would you like to continue with any of these modules?

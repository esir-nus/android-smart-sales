package com.smartsales.business.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE Manager - Main Bluetooth Low Energy Manager
 * 
 * Features:
 * - Device scanning and discovery
 * - Connection management with auto-reconnect
 * - GATT operations (read/write/notify)
 * - Command sending
 * - Status monitoring
 * - Error handling
 * 
 * Usage:
 * ```
 * // Inject via Hilt
 * @Inject lateinit var bleManager: BleManager
 * 
 * // Start scanning
 * bleManager.startScan()
 * 
 * // Observe discovered devices
 * bleManager.discoveredDevices.collect { devices ->
 *     // Update UI
 * }
 * 
 * // Connect to device
 * bleManager.connectDevice(device)
 * 
 * // Send command
 * bleManager.sendCommand(BleCommand.START_RECORDING)
 * ```
 */
@SuppressLint("MissingPermission") // Permissions checked before calling
@Singleton
class BleManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "BleManager"
    }
    
    // ===== BLUETOOTH ADAPTER =====
    
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    private val bluetoothLeScanner: BluetoothLeScanner? by lazy {
        bluetoothAdapter?.bluetoothLeScanner
    }
    
    // ===== STATE MANAGEMENT =====
    
    private val _connectionState = MutableStateFlow<BleConnectionState>(
        BleConnectionState.Disconnected
    )
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    private val _deviceStatus = MutableStateFlow<DeviceStatus?>(null)
    val deviceStatus: StateFlow<DeviceStatus?> = _deviceStatus.asStateFlow()
    
    // ===== GATT CONNECTION =====
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null
    
    // ===== COROUTINE SCOPE =====
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // ===== SCAN MANAGEMENT =====
    
    private var scanJob: Job? = null
    private val scanResults = mutableMapOf<String, BluetoothDevice>()
    
    /**
     * Scan callback for device discovery
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address
            
            // Add to results if not already present
            if (!scanResults.containsKey(address)) {
                scanResults[address] = device
                _discoveredDevices.value = scanResults.values.toList()
                
                Log.d(TAG, "Device found: ${device.name ?: "Unknown"} ($address)")
                
                // Update scanning state with count
                if (_connectionState.value is BleConnectionState.Scanning) {
                    _connectionState.value = BleConnectionState.Scanning(scanResults.size)
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error code: $errorCode")
            _connectionState.value = BleConnectionState.Error(
                "Scan failed: ${getScanErrorMessage(errorCode)}"
            )
        }
        
        private fun getScanErrorMessage(errorCode: Int): String {
            return when (errorCode) {
                SCAN_FAILED_ALREADY_STARTED -> "Scan already started"
                SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed"
                SCAN_FAILED_FEATURE_UNSUPPORTED -> "Feature not supported"
                SCAN_FAILED_INTERNAL_ERROR -> "Internal error"
                else -> "Unknown error ($errorCode)"
            }
        }
    }
    
    // ===== GATT CALLBACK =====
    
    private val gattCallback = object : BluetoothGattCallback() {
        
        override fun onConnectionStateChange(
            gatt: BluetoothGatt,
            status: Int,
            newState: Int
        ) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.value = BleConnectionState.Connected(gatt.device)
                    
                    // Discover services
                    scope.launch {
                        delay(500) // Small delay before service discovery
                        gatt.discoverServices()
                    }
                }
                
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    
                    if (_connectionState.value is BleConnectionState.Ready ||
                        _connectionState.value is BleConnectionState.Connected) {
                        // Unexpected disconnection - attempt reconnect
                        handleUnexpectedDisconnection(gatt.device)
                    } else {
                        _connectionState.value = BleConnectionState.Disconnected
                    }
                    
                    gatt.close()
                    bluetoothGatt = null
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                
                // Verify required service exists
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                if (service != null) {
                    _connectionState.value = BleConnectionState.Ready(gatt.device)
                    Log.d(TAG, "Device ready for operations")
                    
                    // Enable notifications
                    scope.launch {
                        enableNotifications(gatt)
                    }
                } else {
                    _connectionState.value = BleConnectionState.Error(
                        "Required service not found",
                        BleError.ServiceNotFound(),
                        gatt.device
                    )
                }
            } else {
                _connectionState.value = BleConnectionState.Error(
                    "Service discovery failed (status: $status)",
                    device = gatt.device
                )
            }
        }
        
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Characteristic write successful: ${characteristic.uuid}")
            } else {
                Log.e(TAG, "Characteristic write failed: $status")
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                Log.d(TAG, "Characteristic read: ${data.contentToString()}")
                
                // Handle device status response
                if (characteristic.uuid == BleConstants.CHAR_DEVICE_STATUS) {
                    val status = BleConstants.decodeDeviceStatus(data)
                    _deviceStatus.value = status
                    Log.d(TAG, "Device status: ${status.getSummary()}")
                }
            } else {
                Log.e(TAG, "Characteristic read failed: $status")
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            Log.d(TAG, "Notification received: ${data.contentToString()}")
            
            // Handle notifications
            when (characteristic.uuid) {
                BleConstants.CHAR_DEVICE_STATUS -> {
                    val status = BleConstants.decodeDeviceStatus(data)
                    _deviceStatus.value = status
                }
                BleConstants.CHAR_NOTIFICATION -> {
                    handleDeviceNotification(data)
                }
            }
        }
    }
    
    // ===== PUBLIC API =====
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Start BLE scan for devices
     */
    fun startScan() {
        if (!isBluetoothEnabled()) {
            _connectionState.value = BleConnectionState.Error(
                "Bluetooth is not enabled",
                BleError.BluetoothNotEnabled()
            )
            return
        }
        
        // Clear previous results
        scanResults.clear()
        _discoveredDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning(0)
        
        // Build scan filter
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        
        // Build scan settings
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        // Start scan
        bluetoothLeScanner?.startScan(
            listOf(scanFilter),
            scanSettings,
            scanCallback
        )
        
        Log.d(TAG, "BLE scan started")
        
        // Auto-stop scan after timeout
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(BleConstants.SCAN_TIMEOUT_MS)
            stopScan()
        }
    }
    
    /**
     * Stop BLE scan
     */
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
        scanJob?.cancel()
        
        if (_connectionState.value is BleConnectionState.Scanning) {
            _connectionState.value = BleConnectionState.Disconnected
        }
        
        Log.d(TAG, "BLE scan stopped (${scanResults.size} devices found)")
    }
    
    /**
     * Connect to a BLE device
     */
    fun connectDevice(device: BluetoothDevice) {
        // Stop scanning if active
        stopScan()
        
        // Disconnect from current device if any
        disconnect()
        
        currentDevice = device
        _connectionState.value = BleConnectionState.Connecting(device)
        
        Log.d(TAG, "Connecting to ${device.name ?: device.address}")
        
        // Connect with auto-connect = false for faster connection
        bluetoothGatt = device.connectGatt(
            context,
            false, // autoConnect
            gattCallback,
            BluetoothDevice.TRANSPORT_LE
        )
        
        // Setup connection timeout
        scope.launch {
            delay(BleConstants.CONNECTION_TIMEOUT_MS)
            
            if (_connectionState.value is BleConnectionState.Connecting) {
                _connectionState.value = BleConnectionState.Error(
                    "Connection timeout",
                    BleError.ConnectionTimeout(),
                    device
                )
                bluetoothGatt?.disconnect()
            }
        }
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        currentDevice = null
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Disconnected
        _deviceStatus.value = null
        
        Log.d(TAG, "Disconnected")
    }
    
    /**
     * Send WiFi configuration to device
     */
    suspend fun sendWifiConfig(ssid: String, password: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(
                        BleError("Device not ready")
                    )
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError("GATT connection not available")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_WIFI_CONFIG)
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val data = BleConstants.encodeWifiConfig(ssid, password)
                characteristic.value = data
                
                val success = gatt.writeCharacteristic(characteristic)
                
                if (success) {
                    Log.d(TAG, "WiFi config sent successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(BleError.WriteOperationFailed())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send WiFi config", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send command to device
     */
    suspend fun sendCommand(command: BleCommand): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(
                        BleError("Device not ready")
                    )
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError("GATT connection not available")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_COMMAND)
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                characteristic.value = command.toByteArray()
                
                val success = gatt.writeCharacteristic(characteristic)
                
                if (success) {
                    Log.d(TAG, "Command sent: ${command.description}")
                    Result.success(Unit)
                } else {
                    Result.failure(BleError.WriteOperationFailed())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send command", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send display text command
     */
    suspend fun sendDisplayText(text: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(BleError("Device not ready"))
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError("GATT connection not available")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_DISPLAY_CONTROL)
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val data = BleConstants.encodeDisplayText(text)
                characteristic.value = data
                
                val success = gatt.writeCharacteristic(characteristic)
                
                if (success) {
                    Log.d(TAG, "Display text sent: $text")
                    Result.success(Unit)
                } else {
                    Result.failure(BleError.WriteOperationFailed())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send display text", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send display image command
     */
    suspend fun sendDisplayImage(fileName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(BleError("Device not ready"))
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError("GATT connection not available")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_DISPLAY_CONTROL)
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val data = BleConstants.encodeDisplayImage(fileName)
                characteristic.value = data
                
                val success = gatt.writeCharacteristic(characteristic)
                
                if (success) {
                    Log.d(TAG, "Display image sent: $fileName")
                    Result.success(Unit)
                } else {
                    Result.failure(BleError.WriteOperationFailed())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send display image", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Read device status
     */
    suspend fun readDeviceStatus(): Result<DeviceStatus> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(BleError("Device not ready"))
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError("GATT connection not available")
                )
                
                val service = gatt.getService(BleConstants.SERVICE_UUID)
                val characteristic = service?.getCharacteristic(BleConstants.CHAR_DEVICE_STATUS)
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val success = gatt.readCharacteristic(characteristic)
                
                if (success) {
                    // Wait for response (onCharacteristicRead callback)
                    delay(1000)
                    val status = _deviceStatus.value ?: DeviceStatus.unknown()
                    Result.success(status)
                } else {
                    Result.failure(BleError.ReadOperationFailed())
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read device status", e)
                Result.failure(e)
            }
        }
    }
    
    // ===== PRIVATE HELPERS =====
    
    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(BleConstants.SERVICE_UUID) ?: return
        val characteristic = service.getCharacteristic(BleConstants.CHAR_NOTIFICATION) ?: return
        
        // Enable local notifications
        gatt.setCharacteristicNotification(characteristic, true)
        
        // Enable remote notifications
        val descriptor = characteristic.getDescriptor(BleConstants.DESCRIPTOR_CLIENT_CONFIG)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            Log.d(TAG, "Notifications enabled")
        }
    }
    
    private fun handleUnexpectedDisconnection(device: BluetoothDevice) {
        Log.w(TAG, "Unexpected disconnection from ${device.name}")
        
        // Attempt reconnection
        scope.launch {
            for (attempt in 1..BleConstants.MAX_RECONNECT_ATTEMPTS) {
                _connectionState.value = BleConnectionState.Reconnecting(
                    device, attempt, BleConstants.MAX_RECONNECT_ATTEMPTS
                )
                
                delay(BleConstants.RECONNECT_DELAY_MS)
                connectDevice(device)
                
                // Wait to see if connection succeeds
                delay(BleConstants.CONNECTION_TIMEOUT_MS)
                
                if (_connectionState.value is BleConnectionState.Ready) {
                    Log.d(TAG, "Reconnection successful")
                    return@launch
                }
            }
            
            // All reconnection attempts failed
            _connectionState.value = BleConnectionState.Error(
                "Reconnection failed after ${BleConstants.MAX_RECONNECT_ATTEMPTS} attempts",
                BleError.UnexpectedDisconnection(),
                device
            )
        }
    }
    
    private fun handleDeviceNotification(data: ByteArray) {
        // Parse notification data based on your protocol
        Log.d(TAG, "Device notification: ${data.contentToString()}")
        
        // Example: First byte indicates notification type
        if (data.isNotEmpty()) {
            when (data[0].toInt()) {
                0x01 -> Log.d(TAG, "Recording started")
                0x02 -> Log.d(TAG, "Recording stopped")
                0x03 -> Log.d(TAG, "File sync complete")
                0x04 -> Log.d(TAG, "Battery low warning")
                else -> Log.d(TAG, "Unknown notification: ${data[0]}")
            }
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        stopScan()
        disconnect()
        scope.cancel()
    }
}
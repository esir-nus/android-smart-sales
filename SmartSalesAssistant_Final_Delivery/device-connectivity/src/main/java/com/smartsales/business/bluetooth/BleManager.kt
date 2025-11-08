package com.smartsales.business.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothStatusCodes
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import java.nio.charset.Charset
import java.util.UUID
import kotlin.text.Charsets
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

    fun hasConnectPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    fun hasScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
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

    private val _transportEvents = MutableSharedFlow<BleTransportEvent>(
        extraBufferCapacity = 64
    )
    val transportEvents: SharedFlow<BleTransportEvent> = _transportEvents.asSharedFlow()

    private val _networkInfo = MutableSharedFlow<BleNetworkInfo>(
        extraBufferCapacity = 4
    )
    val networkInfo: SharedFlow<BleNetworkInfo> = _networkInfo.asSharedFlow()
    
    // ===== GATT CONNECTION =====
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var currentDevice: BluetoothDevice? = null

    private var negotiatedMtu: Int = BleConstants.DEFAULT_MTU
    private var activeProfile: BleServiceProfile? = null
    
    // ===== COROUTINE SCOPE =====
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // ===== SCAN MANAGEMENT =====
    
    private var scanJob: Job? = null
    private val scanResults = mutableMapOf<String, BluetoothDevice>()

    private fun isTargetDeviceName(name: String?): Boolean {
        return name
            ?.trim()
            ?.equals(BleConstants.TARGET_DEVICE_NAME, ignoreCase = true)
            ?: false
    }

    private fun targetDeviceFound(): Boolean = scanResults.isNotEmpty()
    
    /**
     * Scan callback for device discovery
     */
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: result.scanRecord?.deviceName ?: return
            if (!isTargetDeviceName(name)) return

            val address = device.address
            if (!scanResults.containsKey(address)) {
                scanResults[address] = device
                _discoveredDevices.value = scanResults.values.toList()

                Log.d(TAG, "Target device found: $name ($address)")

                if (_connectionState.value is BleConnectionState.Scanning) {
                    _connectionState.value = BleConnectionState.Scanning(devicesFound = 1)
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val requested = gatt.requestMtu(BleConstants.PREFERRED_MTU)
                            Log.d(TAG, "MTU request (${BleConstants.PREFERRED_MTU}) initiated: $requested")
                        }
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
                val profile = BleConstants.SUPPORTED_SERVICE_PROFILES.firstOrNull { candidate ->
                    gatt.getService(candidate.serviceUuid) != null
                }
                if (profile != null) {
                    activeProfile = profile
                    _connectionState.value = BleConnectionState.Ready(gatt.device)
                    Log.d(TAG, "Device ready for operations using profile: ${profile.name}")
                    
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

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                negotiatedMtu = mtu
                Log.d(TAG, "MTU updated to $mtu")
            } else {
                Log.w(TAG, "MTU change failed with status: $status")
            }
        }
        
        @Suppress("OVERRIDE_DEPRECATION")
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
        
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value
                Log.d(TAG, "Characteristic read: ${data?.contentToString()}")
                
                // Handle device status response
                if (characteristic.uuid == activeProfile?.deviceStatusCharacteristic && data != null) {
                    val deviceStatus = BleConstants.decodeDeviceStatus(data)
                    _deviceStatus.value = deviceStatus
                    Log.d(TAG, "Device status: ${deviceStatus.getSummary()}")
                }
            } else {
                Log.e(TAG, "Characteristic read failed: $status")
            }
        }
        
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            Log.d(TAG, "Notification received: ${data?.contentToString()}")
            
            if (data == null) return
            
            // Handle notifications
            when (characteristic.uuid) {
                activeProfile?.deviceStatusCharacteristic -> {
                    val deviceStatus = BleConstants.decodeDeviceStatus(data)
                    _deviceStatus.value = deviceStatus
                }
                activeProfile?.notificationCharacteristic -> {
                    handleDeviceNotification(data)
                }
            }

            recordTransportEvent(BleTransportDirection.RECEIVED, data)
            processNetworkResponse(data)
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
        if (!hasScanPermission()) {
            _connectionState.value = BleConnectionState.Error(
                "Bluetooth scan permission is not granted",
                BleError.PermissionDenied(Manifest.permission.BLUETOOTH_SCAN.takeIf {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                } ?: Manifest.permission.ACCESS_FINE_LOCATION)
            )
            return
        }

        if (!isBluetoothEnabled()) {
            _connectionState.value = BleConnectionState.Error(
                "Bluetooth is not enabled",
                BleError.BluetoothNotEnabled()
            )
            return
        }
        
        scanJob?.cancel()
        bluetoothLeScanner?.stopScan(scanCallback)

        scanResults.clear()
        _discoveredDevices.value = emptyList()
        _connectionState.value = BleConnectionState.Scanning(devicesFound = 0)
        
        // Build scan filter
        val scanFilters = BleConstants.SCAN_SERVICE_UUIDS
            .distinct()
            .map { uuid ->
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(uuid))
                    .build()
            }
            .ifEmpty {
                listOf(ScanFilter.Builder().build())
            }
        
        // Build scan settings
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        scanJob = scope.launch {
            while (isActive && currentDevice == null) {
                bluetoothLeScanner?.startScan(scanFilters, scanSettings, scanCallback)
                Log.d(TAG, "BLE scan window started (target: ${BleConstants.TARGET_DEVICE_NAME})")

                delay(BleConstants.SCAN_WINDOW_MS)

                bluetoothLeScanner?.stopScan(scanCallback)

                if (!targetDeviceFound()) {
                    _connectionState.value = BleConnectionState.Scanning(devicesFound = 0)
                    Log.d(TAG, "Target not found in this window, retrying...")
                }

                delay(BleConstants.SCAN_RETRY_DELAY_MS)
            }
        }
    }
    
    /**
     * Stop BLE scan
     */
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
        scanJob?.cancel()
        
        if (_connectionState.value is BleConnectionState.Scanning && !_connectionState.value.isConnected()) {
            _connectionState.value = BleConnectionState.Disconnected
        }
        
        Log.d(TAG, "BLE scan stopped (${scanResults.size} target devices cached)")
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
        negotiatedMtu = BleConstants.DEFAULT_MTU
        activeProfile = null
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
        negotiatedMtu = BleConstants.DEFAULT_MTU
        activeProfile = null
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
                        BleError.General("Device not ready")
                    )
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError.General("GATT connection not available")
                )
                
                val characteristic = getCharacteristic(gatt) { it.wifiConfigCharacteristic }
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val payload = BleConstants.encodeWifiConfig(ssid, password)
                val success = writeCharacteristicPayload(
                    gatt = gatt,
                    characteristic = characteristic,
                    data = payload,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )

                if (success) {
                    recordTransportEvent(BleTransportDirection.SENT, payload)
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
     * Send WiFi name/password using BLE text payload (wifi#connect#name#password)
     */
    suspend fun sendWifiAccountCredentials(
        wifiName: String,
        password: String,
        appendNewline: Boolean = true
    ): Result<Unit> {
        val sanitizedAccount = wifiName.replace("#", "").trim()
        val sanitizedPassword = password.replace("#", "")
        val payload = "wifi#connect#$sanitizedAccount#$sanitizedPassword"
        return sendTextPayload(
            text = payload,
            appendNewline = appendNewline,
            writeWithoutResponse = false
        )
    }

    /**
     * Request latest HTTP IP/Wi-Fi account info from gadget.
     */
    suspend fun queryNetworkInfo(): Result<Unit> {
        return sendTextPayload(
            text = BleConstants.NETWORK_QUERY_COMMAND,
            appendNewline = true,
            writeWithoutResponse = false
        )
    }

    /**
     * Send command to device
     */
    suspend fun sendCommand(command: BleCommand): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(
                        BleError.General("Device not ready")
                    )
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError.General("GATT connection not available")
                )
                
                val characteristic = getCharacteristic(gatt) { it.commandCharacteristic }
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val commandPayload = command.toByteArray()
                val success = writeCharacteristicPayload(
                    gatt = gatt,
                    characteristic = characteristic,
                    data = commandPayload,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                
                if (success) {
                    recordTransportEvent(BleTransportDirection.SENT, commandPayload)
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
     * Send arbitrary payload bytes to the connected device.
     * Useful for debugging custom protocols or sending JSON blobs.
     */
    suspend fun sendRawPayload(
        payload: ByteArray,
        writeWithoutResponse: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!_connectionState.value.isReady()) {
                return@withContext Result.failure(BleError.General("Device not ready"))
            }

            val gatt = bluetoothGatt ?: return@withContext Result.failure(
                BleError.General("GATT connection not available")
            )

            val characteristic = getWritableCharacteristic(gatt)
                ?: return@withContext Result.failure(BleError.CharacteristicNotFound())

            val writeType = if (writeWithoutResponse) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }

            val success = writeCharacteristicPayload(
                gatt = gatt,
                characteristic = characteristic,
                data = payload,
                writeType = writeType
            )

            if (success) {
                recordTransportEvent(BleTransportDirection.SENT, payload)
                Result.success(Unit)
            } else {
                Result.failure(BleError.WriteOperationFailed())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send raw payload", e)
            Result.failure(e)
        }
    }

    /**
     * Convenience helper for sending UTF-8 text (strings or JSON) to the gadget.
     */
    suspend fun sendTextPayload(
        text: String,
        charset: Charset = Charsets.UTF_8,
        appendNewline: Boolean = false,
        writeWithoutResponse: Boolean = false
    ): Result<Unit> {
        val payload = buildString {
            append(text)
            if (appendNewline) append('\n')
        }.toByteArray(charset)
        return sendRawPayload(payload, writeWithoutResponse)
    }
    
    /**
     * Send display text command
     */
    suspend fun sendDisplayText(text: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (!_connectionState.value.isReady()) {
                    return@withContext Result.failure(BleError.General("Device not ready"))
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError.General("GATT connection not available")
                )
                
                val characteristic = getCharacteristic(gatt) { it.displayControlCharacteristic }
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val data = BleConstants.encodeDisplayText(text)
                val success = writeCharacteristicPayload(
                    gatt = gatt,
                    characteristic = characteristic,
                    data = data,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                
                if (success) {
                    recordTransportEvent(BleTransportDirection.SENT, data)
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
                    return@withContext Result.failure(BleError.General("Device not ready"))
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError.General("GATT connection not available")
                )
                
                val characteristic = getCharacteristic(gatt) { it.displayControlCharacteristic }
                
                if (characteristic == null) {
                    return@withContext Result.failure(BleError.CharacteristicNotFound())
                }
                
                val data = BleConstants.encodeDisplayImage(fileName)
                val success = writeCharacteristicPayload(
                    gatt = gatt,
                    characteristic = characteristic,
                    data = data,
                    writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
                
                if (success) {
                    recordTransportEvent(BleTransportDirection.SENT, data)
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
                    return@withContext Result.failure(BleError.General("Device not ready"))
                }
                
                val gatt = bluetoothGatt ?: return@withContext Result.failure(
                    BleError.General("GATT connection not available")
                )
                
                val characteristic = getCharacteristic(gatt) { it.deviceStatusCharacteristic }
                
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

    private suspend fun writeCharacteristicPayload(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray,
        writeType: Int
    ): Boolean {
        if (data.isEmpty()) return true

        val payloadSize = (negotiatedMtu - BleConstants.GATT_MTU_OVERHEAD)
            .coerceAtLeast(BleConstants.DEFAULT_CHUNK_SIZE)

        val chunks = BleConstants.chunkData(data, payloadSize)
        for ((index, chunk) in chunks.withIndex()) {
            val success = writeCharacteristicChunk(gatt, characteristic, chunk, writeType)
            if (!success) {
                Log.e(TAG, "Failed to write BLE chunk ${index + 1}/${chunks.size}")
                return false
            }
            if (chunks.size > 1) {
                delay(BleConstants.CHUNK_WRITE_DELAY_MS)
            }
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun writeCharacteristicChunk(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        chunk: ByteArray,
        writeType: Int
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeCharacteristic(characteristic, chunk, writeType) == BluetoothStatusCodes.SUCCESS
        } else {
            characteristic.writeType = writeType
            characteristic.value = chunk
            gatt.writeCharacteristic(characteristic)
        }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptorCompat(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS
        } else {
            descriptor.value = value
            gatt.writeDescriptor(descriptor)
        }
    }

    private fun getCharacteristic(
        gatt: BluetoothGatt,
        selector: (BleServiceProfile) -> UUID?
    ): BluetoothGattCharacteristic? {
        val profile = activeProfile ?: return null
        val characteristicUuid = selector(profile) ?: return null
        val service = gatt.getService(profile.serviceUuid) ?: return null
        return service.getCharacteristic(characteristicUuid)
    }

    private fun getWritableCharacteristic(gatt: BluetoothGatt): BluetoothGattCharacteristic? {
        return getCharacteristic(gatt) { it.commandCharacteristic }
            ?: getCharacteristic(gatt) { it.wifiConfigCharacteristic }
            ?: getCharacteristic(gatt) { it.displayControlCharacteristic }
    }

    private fun recordTransportEvent(direction: BleTransportDirection, payload: ByteArray) {
        val snapshot = payload.copyOf()
        scope.launch {
            _transportEvents.emit(BleTransportEvent(direction, snapshot))
        }
    }

    private fun processNetworkResponse(payload: ByteArray) {
        val message = payload.toString(Charsets.UTF_8).trim()
        if (!message.startsWith(BleConstants.NETWORK_RESPONSE_PREFIX)) return
        val parts = message.split("#")
        if (parts.size < 4) return
        val ip = parts[2].trim()
        val wifiName = parts[3].trim()
        if (ip.isEmpty()) return
        scope.launch {
            _networkInfo.emit(BleNetworkInfo(ip, wifiName))
        }
    }
    
    private fun enableNotifications(gatt: BluetoothGatt) {
        val characteristic = getCharacteristic(gatt) { it.notificationCharacteristic } ?: return
        
        // Enable local notifications
        gatt.setCharacteristicNotification(characteristic, true)
        
        // Enable remote notifications
        val descriptor = characteristic.getDescriptor(BleConstants.DESCRIPTOR_CLIENT_CONFIG)
        if (descriptor != null) {
            val success = writeDescriptorCompat(
                gatt,
                descriptor,
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            )
            if (success) {
                Log.d(TAG, "Notifications enabled")
            } else {
                Log.w(TAG, "Failed to enable notifications on descriptor ${descriptor.uuid}")
            }
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

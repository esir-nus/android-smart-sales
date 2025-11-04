package com.smartsales.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for BLE operations with Smart Sales Gadget
 * Handles scanning, connecting, and communication
 */
@SuppressLint("MissingPermission")
@Singleton
class BleManager @Inject constructor(
    private val context: Context
) {
    
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
                Log.d(TAG, "Found device: ${device.name} (${device.address})")
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed with error: $errorCode")
            _connectionState.value = BleConnectionState.Error("Scan failed: $errorCode")
        }
    }
    
    // GATT callback
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.d(TAG, "Connected to GATT server")
                    _connectionState.value = BleConnectionState.Connected(gatt.device.address)
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.d(TAG, "Disconnected from GATT server")
                    _connectionState.value = BleConnectionState.Disconnected
                    gatt.close()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Services discovered")
                _connectionState.value = BleConnectionState.ServicesDiscovered(gatt.device.address)
            } else {
                Log.e(TAG, "Service discovery failed: $status")
                _connectionState.value = BleConnectionState.Error("Service discovery failed")
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
                Log.d(TAG, "Characteristic read: ${data?.contentToString()}")
            }
        }
    }
    
    /**
     * Start scanning for BLE devices
     */
    fun startScan() {
        Log.d(TAG, "Starting BLE scan")
        _discoveredDevices.value = emptyList()
        
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }
    
    /**
     * Stop scanning for BLE devices
     */
    fun stopScan() {
        Log.d(TAG, "Stopping BLE scan")
        bluetoothLeScanner?.stopScan(scanCallback)
    }
    
    /**
     * Connect to a BLE device
     */
    fun connectDevice(device: BluetoothDevice) {
        Log.d(TAG, "Connecting to device: ${device.address}")
        bluetoothGatt?.close()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
    
    /**
     * Disconnect from current device
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from device")
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }
    
    /**
     * Send WiFi configuration to device
     */
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
                        delay(500) // Wait for write to complete
                        Log.d(TAG, "WiFi config sent successfully")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to write WiFi config"))
                    }
                } else {
                    Result.failure(Exception("WiFi config characteristic not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending WiFi config", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Send a command to the device
     */
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
                        delay(500)
                        Log.d(TAG, "Command sent successfully: $command")
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Failed to send command"))
                    }
                } else {
                    Result.failure(Exception("Command characteristic not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Read device status
     */
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
                        delay(500) // Wait for read to complete
                        val data = characteristic.value
                        Log.d(TAG, "Device status read successfully")
                        Result.success(data)
                    } else {
                        Result.failure(Exception("Failed to read device status"))
                    }
                } else {
                    Result.failure(Exception("Device status characteristic not found"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading device status", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if Bluetooth is enabled
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    companion object {
        private const val TAG = "BleManager"
    }
}

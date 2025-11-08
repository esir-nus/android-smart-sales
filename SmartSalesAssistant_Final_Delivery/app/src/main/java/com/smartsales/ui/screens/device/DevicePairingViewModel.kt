package com.smartsales.ui.screens.device

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.business.bluetooth.BleManager
import com.smartsales.data.local.entity.DeviceSettingEntity
import com.smartsales.data.local.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Device Pairing UI State
 */
data class DevicePairingUiState(
    val devices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val error: String? = null
)

/**
 * Device Pairing ViewModel
 */
@HiltViewModel
class DevicePairingViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DevicePairingUiState())
    val uiState: StateFlow<DevicePairingUiState> = _uiState.asStateFlow()
    
    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    
    init {
        observeDiscoveredDevices()
        observeConnectionState()
    }
    
    private fun observeDiscoveredDevices() {
        viewModelScope.launch {
            bleManager.discoveredDevices.collect { devices ->
                _uiState.update {
                    it.copy(devices = devices)
                }
            }
        }
    }
    
    private fun observeConnectionState() {
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        isScanning = state.isScanning(),
                        error = if (state is BleConnectionState.Error) state.message else null
                    )
                }
                
                // Save device when connected
                if (state is BleConnectionState.Ready) {
                    saveConnectedDevice(state.device)
                }
            }
        }
    }
    
    /**
     * Start BLE scan
     */
    fun startScan() {
        if (!bleManager.hasScanPermission()) {
            _uiState.update {
                it.copy(error = "请先授予蓝牙扫描权限")
            }
            return
        }

        if (!bleManager.isBluetoothEnabled()) {
            _uiState.update {
                it.copy(error = "请先开启蓝牙")
            }
            return
        }
        
        bleManager.startScan()
    }
    
    /**
     * Stop BLE scan
     */
    fun stopScan() {
        bleManager.stopScan()
    }
    
    /**
     * Connect to device
     */
    fun connectDevice(device: BluetoothDevice) {
        bleManager.connectDevice(device)
    }
    
    /**
     * Disconnect from device
     */
    fun disconnect() {
        bleManager.disconnect()
    }
    
    /**
     * Send WiFi configuration to device
     */
    fun sendWifiConfig(ssid: String, password: String) {
        viewModelScope.launch {
            val result = bleManager.sendWifiConfig(ssid, password)
            
            result.onSuccess {
                // Save WiFi config to database
                deviceRepository.saveWifiConfig(ssid, password)
            }
            
            result.onFailure { exception ->
                _uiState.update {
                    it.copy(error = "WiFi配置失败: ${exception.message}")
                }
            }
        }
    }
    
    /**
     * Save connected device to database
     */
    private fun saveConnectedDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            if (!bleManager.hasConnectPermission()) {
                return@launch
            }

            runCatching {
                val deviceId = device.address
                val deviceName = device.name ?: "Unknown Device"

                DeviceSettingEntity.create(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    isPaired = true
                )
            }.onSuccess { deviceSetting ->
                runCatching {
                    deviceRepository.saveDevice(deviceSetting)
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}

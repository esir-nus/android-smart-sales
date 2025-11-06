package com.smartsales.wifibletest.ui

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.business.bluetooth.BleManager
import com.smartsales.data.network.ConnectivityApiConfig
import com.smartsales.data.network.GadgetApiFactory
import com.smartsales.data.network.api.GadgetApiHelper
import com.smartsales.data.network.model.DeviceStatusResponse
import com.smartsales.data.network.model.GadgetFile
import com.smartsales.data.network.api.ConnectivityStatus
import com.smartsales.wifibletest.data.SavedWifiConfig
import com.smartsales.wifibletest.data.WifiConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WifiBleTestUiState(
    val devices: List<BluetoothDevice> = emptyList(),
    val wifiConfigs: List<SavedWifiConfig> = emptyList(),
    val isScanning: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isSendingWifi: Boolean = false,
    val deviceIp: String = "",
    val devicePort: String = ConnectivityApiConfig.DEFAULT_PORT.toString(),
    val files: List<GadgetFile> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val fileErrorMessage: String? = null,
    val connectionStatus: ConnectivityStatus? = null,
    val httpDeviceStatus: DeviceStatusResponse? = null
)

@HiltViewModel
class WifiBleTestViewModel @Inject constructor(
    private val bleManager: BleManager,
    private val wifiConfigRepository: WifiConfigRepository,
    private val gadgetApiFactory: GadgetApiFactory
) : ViewModel() {

    private val _uiState = MutableStateFlow(WifiBleTestUiState())
    val uiState: StateFlow<WifiBleTestUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
    val deviceStatus = bleManager.deviceStatus

    init {
        observeDiscoveredDevices()
        observeConnectionState()
        observeWifiConfigs()
        observeDeviceEndpoint()
    }

    private fun observeDiscoveredDevices() {
        viewModelScope.launch {
            bleManager.discoveredDevices.collectLatest { devices ->
                _uiState.update { state ->
                    state.copy(devices = devices)
                }
            }
        }
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            bleManager.connectionState.collectLatest { state ->
                _uiState.update { current ->
                    current.copy(
                        isScanning = state.isScanning(),
                        errorMessage = if (state is BleConnectionState.Error) {
                            state.message
                        } else {
                            null
                        },
                        infoMessage = when (state) {
                            is BleConnectionState.Ready ->
                                "Connected to ${state.device.name ?: state.device.address}"
                            is BleConnectionState.Connecting -> "Connecting..."
                            is BleConnectionState.Disconnected -> "Disconnected"
                            else -> current.infoMessage
                        }
                    )
                }
            }
        }
    }

    private fun observeWifiConfigs() {
        viewModelScope.launch {
            wifiConfigRepository.configs.collectLatest { configs ->
                _uiState.update { state ->
                    state.copy(wifiConfigs = configs)
                }
            }
        }
    }

    private fun observeDeviceEndpoint() {
        viewModelScope.launch {
            wifiConfigRepository.deviceIp.collectLatest { ip ->
                _uiState.update { state -> state.copy(deviceIp = ip) }
            }
        }
        viewModelScope.launch {
            wifiConfigRepository.devicePort.collectLatest { port ->
                _uiState.update { state -> state.copy(devicePort = port.toString()) }
            }
        }
    }

    fun startScan() {
        if (!bleManager.isBluetoothEnabled()) {
            _uiState.update { state ->
                state.copy(errorMessage = "请先开启蓝牙")
            }
            return
        }
        bleManager.startScan()
        _uiState.update { it.copy(infoMessage = "正在扫描附近设备...", errorMessage = null) }
    }

    fun stopScan() {
        bleManager.stopScan()
        _uiState.update { it.copy(infoMessage = "扫描已停止") }
    }

    fun connect(device: BluetoothDevice) {
        bleManager.connectDevice(device)
        _uiState.update { it.copy(infoMessage = "正在连接 ${device.name ?: device.address}") }
    }

    fun disconnect() {
        bleManager.disconnect()
        _uiState.update { it.copy(infoMessage = "已断开连接") }
    }

    fun sendWifiConfig(ssid: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSendingWifi = true, errorMessage = null) }

            val result = bleManager.sendWifiConfig(ssid, password)

            result.onSuccess {
                wifiConfigRepository.saveWifiConfig(ssid, password, setAsDefault = true)
                _uiState.update {
                    it.copy(
                        isSendingWifi = false,
                        infoMessage = "WiFi 配置已发送",
                        errorMessage = null
                    )
                }
            }

            result.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSendingWifi = false,
                        errorMessage = "WiFi配置失败: ${throwable.message ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun onDeviceIpChanged(ip: String) {
        _uiState.update { it.copy(deviceIp = ip) }
    }

    fun onDevicePortChanged(port: String) {
        val sanitized = port.filter { it.isDigit() }.take(5)
        _uiState.update { it.copy(devicePort = sanitized) }
    }

    fun loadDeviceFiles() {
        val currentState = _uiState.value
        val ip = currentState.deviceIp.trim()
        if (ip.isBlank()) {
            _uiState.update { it.copy(fileErrorMessage = "请先输入设备 IP 地址") }
            return
        }

        val port = currentState.devicePort.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: ConnectivityApiConfig.DEFAULT_PORT

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingFiles = true,
                    fileErrorMessage = null,
                    infoMessage = null
                )
            }

            wifiConfigRepository.updateDeviceEndpoint(ip, port)

            val api = gadgetApiFactory.create(ip, port)

            try {
                val connectivity = GadgetApiHelper.validateConnectivity(api)
                _uiState.update { it.copy(connectionStatus = connectivity) }

                if (!connectivity.reachable) {
                    _uiState.update {
                        it.copy(
                            isLoadingFiles = false,
                            fileErrorMessage = connectivity.error ?: "设备不可达"
                        )
                    }
                    return@launch
                }

                val fileResponse = api.getFileList()
                val files = if (fileResponse.isSuccessful) {
                    fileResponse.body()?.files ?: emptyList()
                } else {
                    emptyList()
                }

                val statusResponse = api.getDeviceStatus()
                val deviceStatus = if (statusResponse.isSuccessful) {
                    statusResponse.body()
                } else null

                _uiState.update {
                    it.copy(
                        isLoadingFiles = false,
                        files = files,
                        httpDeviceStatus = deviceStatus,
                        fileErrorMessage = if (fileResponse.isSuccessful) null else "获取文件失败: ${fileResponse.code()}",
                        infoMessage = if (fileResponse.isSuccessful) {
                            "文件列表已更新（${files.size}）"
                        } else it.infoMessage
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingFiles = false,
                        fileErrorMessage = e.message ?: "加载失败"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                infoMessage = null,
                errorMessage = null,
                fileErrorMessage = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        bleManager.stopScan()
    }
}

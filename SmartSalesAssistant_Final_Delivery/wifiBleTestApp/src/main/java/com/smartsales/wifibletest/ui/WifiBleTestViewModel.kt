package com.smartsales.wifibletest.ui

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.net.wifi.WifiManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.business.bluetooth.BleManager
import com.smartsales.business.bluetooth.BleTransportDirection
import com.smartsales.data.network.ConnectivityApiConfig
import com.smartsales.wifibletest.data.SavedWifiConfig
import com.smartsales.wifibletest.data.WifiConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.text.Charsets

data class WifiBleTestUiState(
    val devices: List<BluetoothDevice> = emptyList(),
    val wifiConfigs: List<SavedWifiConfig> = emptyList(),
    val isScanning: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
    val isSendingWifi: Boolean = false,
    val deviceIp: String = "",
    val devicePort: String = ConnectivityApiConfig.DEFAULT_PORT.toString(),
    val gadgetWifiName: String? = null,
    val phoneWifiName: String? = null,
    val wifiNetworkMatch: Boolean? = null,
    val isQueryingNetwork: Boolean = false,
    val transportLog: List<BleTransportLogEntry> = emptyList(),
    val webConsoleUrl: String? = null,
    val webConsoleError: String? = null,
    val isMediaServer: Boolean = false, // ANDROID INTEGRATION: Detect media server
    val serverType: String = "unknown", // ANDROID INTEGRATION: "gadget_api" or "media_server"
)

data class BleTransportLogEntry(
    val id: Long,
    val direction: BleTransportDirection,
    val textPreview: String,
    val hexPreview: String,
    val timestamp: Long,
)

@HiltViewModel
class WifiBleTestViewModel
    @Inject
    constructor(
        private val bleManager: BleManager,
        private val wifiConfigRepository: WifiConfigRepository,
        @ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(WifiBleTestUiState())
        val uiState: StateFlow<WifiBleTestUiState> = _uiState.asStateFlow()

        val connectionState: StateFlow<BleConnectionState> = bleManager.connectionState
        val deviceStatus = bleManager.deviceStatus

        init {
            refreshPhoneWifiName()
            observeDiscoveredDevices()
            observeConnectionState()
            observeWifiConfigs()
            observeDeviceEndpoint()
            observeTransportEvents()
            observeNetworkInfo()
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
                            errorMessage =
                                if (state is BleConnectionState.Error) {
                                    state.message
                                } else {
                                    null
                                },
                            infoMessage =
                                when (state) {
                                    is BleConnectionState.Ready ->
                                        "Connected to ${state.device.name ?: state.device.address}"
                                    is BleConnectionState.Connecting -> "Connecting..."
                                    is BleConnectionState.Disconnected -> "Disconnected"
                                    else -> current.infoMessage
                                },
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

        private fun observeTransportEvents() {
            viewModelScope.launch {
                bleManager.transportEvents.collect { event ->
                    val textPreview =
                        event.payload
                            .toString(Charsets.UTF_8)
                            .replace("\n", "\\n")
                            .replace("\r", "")
                            .ifBlank { "(binary)" }
                            .take(80)
                    val hexPreview =
                        event.payload.joinToString(" ") {
                            it.toUByte().toString(16).padStart(2, '0')
                        }.take(120)

                    val entry =
                        BleTransportLogEntry(
                            id = event.timestamp,
                            direction = event.direction,
                            textPreview = textPreview,
                            hexPreview = hexPreview,
                            timestamp = event.timestamp,
                        )

                    _uiState.update { state ->
                        val updated = (listOf(entry) + state.transportLog).take(20)
                        state.copy(transportLog = updated)
                    }
                }
            }
        }

        private fun observeNetworkInfo() {
            viewModelScope.launch {
                bleManager.networkInfo.collect { info ->
                    val parsed = parseGadgetAddress(info.httpIp)
                    val host = parsed.host.ifBlank { info.httpIp }
                    val port = parsed.port ?: ConnectivityApiConfig.DEFAULT_PORT

                    wifiConfigRepository.updateDeviceEndpoint(host, port)

                    val phoneWifiName = resolvePhoneWifiName() ?: _uiState.value.phoneWifiName
                    val gadgetWifiName = info.wifiName.ifBlank { null }
                    val matchResult =
                        when {
                            phoneWifiName.isNullOrBlank() || gadgetWifiName.isNullOrBlank() -> null
                            else -> gadgetWifiName.equals(phoneWifiName, ignoreCase = true)
                        }
                    val mismatchMessage =
                        if (matchResult == false && phoneWifiName != null && gadgetWifiName != null) {
                            "设备 WiFi 名称 ($gadgetWifiName) 与手机 ($phoneWifiName) 不一致，请重新输入。"
                        } else {
                            null
                        }
                    _uiState.update {
                        it.copy(
                            deviceIp = host,
                            devicePort = port.toString(),
                            gadgetWifiName = gadgetWifiName,
                            phoneWifiName = phoneWifiName,
                            wifiNetworkMatch = matchResult,
                            infoMessage = "设备网络: http://$host:$port/",
                            errorMessage = mismatchMessage ?: it.errorMessage,
                        )
                    }
                }
            }
        }

        fun startScan() {
            if (!bleManager.hasScanPermission()) {
                _uiState.update { state ->
                    state.copy(errorMessage = "请先授予蓝牙扫描权限")
                }
                return
            }

            if (!bleManager.isBluetoothEnabled()) {
                _uiState.update { state ->
                    state.copy(errorMessage = "请先开启蓝牙")
                }
                return
            }
            bleManager.startScan()
            _uiState.update { it.copy(infoMessage = "正在搜索 BT311...", errorMessage = null) }
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

        fun queryNetworkInfo() {
            viewModelScope.launch {
                _uiState.update { it.copy(isQueryingNetwork = true, errorMessage = null) }
                val result = bleManager.queryNetworkInfo()
                _uiState.update { it.copy(isQueryingNetwork = false) }
                result.onFailure { throwable ->
                    _uiState.update {
                        it.copy(errorMessage = "查询网络失败: ${throwable.message ?: "未知错误"}")
                    }
                }
            }
        }

        fun sendWifiConfig(
            wifiName: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSendingWifi = true, errorMessage = null) }

                val result = bleManager.sendWifiAccountCredentials(wifiName, password)

                result.onSuccess {
                    wifiConfigRepository.saveWifiConfig(wifiName, password, setAsDefault = true)
                    _uiState.update {
                        it.copy(
                            isSendingWifi = false,
                            infoMessage = "WiFi 名称已下发",
                            errorMessage = null,
                        )
                    }
                }

                result.onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSendingWifi = false,
                            errorMessage = "WiFi配置失败: ${throwable.message ?: "未知错误"}",
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

        /**
         * ANDROID INTEGRATION: Detect server type (gadget API vs media server)
         */
        private suspend fun detectServerType(
            ip: String,
            port: Int,
        ): String {
            return try {
                // Try to detect if it's a media server by checking for the JSON API endpoint
                val url = ConnectivityApiConfig.buildBaseUrl(ip, port)
                return kotlinx.coroutines.withTimeout(3000) {
                    // Simple HTTP check for media server JSON response
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    connection.connect()

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        if (responseBody.contains("\"android_integration\": true") ||
                            responseBody.contains("\"server\": \"media_server\"")
                        ) {
                            "media_server"
                        } else {
                            "gadget_api"
                        }
                    } else {
                        "gadget_api" // Default to gadget API
                    }
                }
            } catch (e: Exception) {
                "gadget_api" // Default to gadget API if detection fails
            }
        }

        fun openWebConsole(): String? {
            val currentState = _uiState.value
            val parsedInput = parseGadgetAddress(currentState.deviceIp)
            val ip = parsedInput.host.ifBlank { currentState.deviceIp.trim() }
            if (ip.isBlank()) {
                _uiState.update { it.copy(webConsoleError = "请先输入设备 IP 地址") }
                return null
            }

            val prioritizedPort = parsedInput.port ?: currentState.devicePort.toIntOrNull()
            val port =
                prioritizedPort?.takeIf { it in 1..65535 }
                    ?: ConnectivityApiConfig.DEFAULT_PORT

            viewModelScope.launch {
                wifiConfigRepository.updateDeviceEndpoint(ip, port)

                // ANDROID INTEGRATION: Detect server type
                val serverType = detectServerType(ip, port)
                val isMediaServer = serverType == "media_server"

                val url =
                    if (isMediaServer) {
                        // For media server, point directly to the web UI
                        ConnectivityApiConfig.buildBaseUrl(ip, port)
                    } else {
                        // For gadget API, use the existing console URL logic
                        ConnectivityApiConfig.buildBaseUrl(ip, port)
                    }

                _uiState.update {
                    it.copy(
                        deviceIp = ip,
                        devicePort = port.toString(),
                        webConsoleUrl = url,
                        webConsoleError = null,
                        isMediaServer = isMediaServer,
                        serverType = serverType,
                        infoMessage =
                            if (isMediaServer) {
                                "检测到媒体服务器，正在加载媒体管理界面..."
                            } else {
                                "正在连接设备控制台..."
                            },
                    )
                }
            }

            return _uiState.value.webConsoleUrl
        }

        /**
         * ANDROID INTEGRATION: Open media server directly
         * Uses port 34123 for local testing (was 8000 for real hardware)
         */
        fun openMediaServer(): String? {
            val currentState = _uiState.value
            val parsedInput = parseGadgetAddress(currentState.deviceIp)
            val ip = parsedInput.host.ifBlank { currentState.deviceIp.trim() }
            if (ip.isBlank()) {
                _uiState.update { it.copy(webConsoleError = "请先输入设备 IP 地址") }
                return null
            }

            // Force port 34123 for local testing media server (was 8000 for real hardware)
            val port = 34123
            val url = ConnectivityApiConfig.buildBaseUrl(ip, port)

            viewModelScope.launch {
                wifiConfigRepository.updateDeviceEndpoint(ip, port)

                _uiState.update {
                    it.copy(
                        deviceIp = ip,
                        devicePort = port.toString(),
                        webConsoleUrl = url,
                        webConsoleError = null,
                        isMediaServer = true,
                        serverType = "media_server",
                        infoMessage = "正在连接媒体服务器...",
                    )
                }
            }

            return url
        }

        fun closeWebConsole() {
            _uiState.update { it.copy(webConsoleUrl = null) }
        }

        fun clearMessages() {
            _uiState.update {
                it.copy(
                    infoMessage = null,
                    errorMessage = null,
                    webConsoleError = null,
                )
            }
        }

        private fun refreshPhoneWifiName(): String? {
            val wifiName = resolvePhoneWifiName()
            _uiState.update { state ->
                if (state.phoneWifiName == wifiName) state else state.copy(phoneWifiName = wifiName)
            }
            return wifiName
        }

        private fun resolvePhoneWifiName(): String? {
            return try {
                val wifiManager =
                    appContext.applicationContext
                        .getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val ssid = wifiManager?.connectionInfo?.ssid?.trim('"')
                if (ssid.isNullOrBlank() || ssid == WifiManager.UNKNOWN_SSID) {
                    null
                } else {
                    ssid
                }
            } catch (securityException: SecurityException) {
                null
            }
        }

        private data class GadgetAddress(
            val host: String,
            val port: Int?,
        )

        private fun parseGadgetAddress(raw: String): GadgetAddress {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return GadgetAddress("", null)

            val withoutScheme =
                when {
                    trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substringAfter("://")
                    trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substringAfter("://")
                    else -> trimmed
                }

            val hostPort = withoutScheme.substringBefore("/")
            val host = hostPort.substringBefore(":").trim()
            val port =
                hostPort.substringAfter(":", "")
                    .takeIf { it.isNotBlank() }
                    ?.toIntOrNull()

            return GadgetAddress(host = host, port = port)
        }

        override fun onCleared() {
            super.onCleared()
            bleManager.stopScan()
        }
    }

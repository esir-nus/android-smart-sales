package com.smartsales.wifibletest.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.business.bluetooth.DeviceStatus
import com.smartsales.data.network.api.ConnectivityStatus
import com.smartsales.data.network.model.DeviceStatusResponse
import com.smartsales.data.network.model.GadgetFile
import com.smartsales.wifibletest.data.SavedWifiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiBleTestScreen(
    modifier: Modifier = Modifier,
    viewModel: WifiBleTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage, uiState.fileErrorMessage) {
        when {
            uiState.errorMessage != null -> {
                snackbarHostState.showSnackbar(uiState.errorMessage, duration = SnackbarDuration.Long)
                viewModel.clearMessages()
            }
            uiState.fileErrorMessage != null -> {
                snackbarHostState.showSnackbar(uiState.fileErrorMessage, duration = SnackbarDuration.Long)
                viewModel.clearMessages()
            }
            uiState.infoMessage != null -> {
                snackbarHostState.showSnackbar(uiState.infoMessage, duration = SnackbarDuration.Short)
                viewModel.clearMessages()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("WiFi & BLE Tester") },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = "Start Scan")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ConnectionStatusCard(
                    connectionState = connectionState,
                    deviceStatus = deviceStatus,
                    onStartScan = viewModel::startScan,
                    onStopScan = viewModel::stopScan,
                    onDisconnect = viewModel::disconnect
                )
            }

            item {
                DeviceListSection(
                    devices = uiState.devices,
                    onConnect = viewModel::connect
                )
            }

            item {
                WifiConfigSection(
                    isSending = uiState.isSendingWifi,
                    configs = uiState.wifiConfigs,
                    onSend = viewModel::sendWifiConfig
                )
            }

            item {
                HttpFileBrowserSection(
                    deviceIp = uiState.deviceIp,
                    devicePort = uiState.devicePort,
                    isLoading = uiState.isLoadingFiles,
                    files = uiState.files,
                    connectionStatus = uiState.connectionStatus,
                    deviceStatus = uiState.httpDeviceStatus,
                    onIpChanged = viewModel::onDeviceIpChanged,
                    onPortChanged = viewModel::onDevicePortChanged,
                    onRefresh = viewModel::loadDeviceFiles
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BleConnectionState,
    deviceStatus: DeviceStatus?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "当前状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(text = connectionState.getStatusMessage())

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onStartScan) {
                    Text("开始扫描")
                }
                OutlinedButton(onClick = onStopScan) {
                    Text("停止扫描")
                }
                if (connectionState.isReady()) {
                    TextButton(onClick = onDisconnect) {
                        Text("断开连接")
                    }
                }
            }

            deviceStatus?.let { status ->
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "设备状态",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(text = "电量: ${status.batteryLevel}%")
                Text(text = "WiFi 已连接: ${if (status.isWifiConnected) "是" else "否"}")
                Text(text = "录音状态: ${if (status.isRecording) "进行中" else "未开启"}")
            }
        }
    }
}

@Composable
private fun DeviceListSection(
    devices: List<BluetoothDevice>,
    onConnect: (BluetoothDevice) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "发现的设备 (${devices.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (devices.isEmpty()) {
                Text("暂无设备，请点击“开始扫描”。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    devices.forEach { device ->
                        DeviceRow(device = device, onConnect = onConnect)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDevice,
    onConnect: (BluetoothDevice) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = device.name ?: "未命名设备",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(text = device.address, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = { onConnect(device) }) {
            Text("连接")
        }
    }
}

@Composable
private fun WifiConfigSection(
    isSending: Boolean,
    configs: List<SavedWifiConfig>,
    onSend: (String, String) -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "WiFi 配置",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            var ssid by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = ssid,
                onValueChange = { ssid = it },
                label = { Text("SSID") },
                singleLine = true,
                trailingIcon = {
                    Icon(Icons.Filled.Wifi, contentDescription = null)
                }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                singleLine = true
            )

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (ssid.isNotBlank()) {
                        onSend(ssid, password)
                    }
                },
                enabled = !isSending && ssid.isNotBlank()
            ) {
                Text(if (isSending) "发送中..." else "发送到设备")
            }

            if (configs.isNotEmpty()) {
                Divider()
                Text(
                    text = "已保存网络",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    configs.forEach { config ->
                        AssistChip(
                            onClick = { onSend(config.ssid, config.password) },
                            label = { Text(config.ssid) },
                            leadingIcon = if (config.isDefault) {
                                {
                                    Icon(
                                        Icons.Filled.Star,
                                        contentDescription = "默认网络"
                                    )
                                }
                            } else null
                        )
                    }
                }
            } else {
                Text(
                    text = "暂未保存任何网络配置。",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun HttpFileBrowserSection(
    deviceIp: String,
    devicePort: String,
    isLoading: Boolean,
    files: List<GadgetFile>,
    connectionStatus: ConnectivityStatus?,
    deviceStatus: DeviceStatusResponse?,
    onIpChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onRefresh: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "设备文件 (HTTP)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = deviceIp,
                onValueChange = onIpChanged,
                label = { Text("设备 IP 地址") },
                singleLine = true,
                placeholder = { Text("例如 192.168.4.1") }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = devicePort,
                onValueChange = onPortChanged,
                label = { Text("端口") },
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onRefresh,
                    enabled = !isLoading && deviceIp.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("加载中")
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("加载设备文件")
                    }
                }
            }

            connectionStatus?.let { status ->
                val statusText = when {
                    !status.reachable -> "设备不可达"
                    !status.wifiConnected -> "HTTP已连接，但设备未连接 Wi-Fi"
                    else -> "设备在线"
                }
                val statusColor = if (status.reachable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Text(
                    text = statusText,
                    color = statusColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                status.error?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            deviceStatus?.let { status ->
                Divider()
                Text(
                    text = "设备状态",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text("电量: ${status.batteryLevel}%")
                Text("Wi-Fi: ${if (status.wifiConnected) "已连接 (${status.wifiSsid ?: "未知"})" else "未连接"}")
                Text("存储: ${status.getFormattedStorageUsed()} / ${status.getFormattedStorageTotal()}")
                Text("固件版本: ${status.firmwareVersion}")
            }

            Divider()

            if (isLoading) {
                Text("正在获取文件列表...")
            } else if (files.isEmpty()) {
                Text(
                    text = "暂未获取到设备文件，请确认设备已连接同一 Wi-Fi 网络。",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    files.forEach { file ->
                        GadgetFileRow(file = file)
                    }
                }
            }
        }
    }
}

@Composable
private fun GadgetFileRow(file: GadgetFile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "${file.getFormattedSize()} · ${file.type} · ${if (file.synced) "已同步" else "未同步"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

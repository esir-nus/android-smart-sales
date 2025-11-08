package com.smartsales.wifibletest.ui

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.business.bluetooth.DeviceStatus
import com.smartsales.wifibletest.data.SavedWifiConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiBleTestScreen(
    modifier: Modifier = Modifier,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onNavigateToWebConsole: () -> Unit,
    viewModel: WifiBleTestViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val deviceStatus by viewModel.deviceStatus.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val handleOpenWebConsole: () -> Unit = {
        val url = viewModel.openWebConsole()
        if (url != null) {
            onNavigateToWebConsole()
        }
    }

    LaunchedEffect(uiState.infoMessage, uiState.errorMessage, uiState.webConsoleError) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
            return@LaunchedEffect
        }
        uiState.webConsoleError?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            viewModel.clearMessages()
            return@LaunchedEffect
        }
        uiState.infoMessage?.let { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            viewModel.clearMessages()
        }
    }

    val handleStartScan: () -> Unit = {
        if (hasPermissions) {
            viewModel.startScan()
        } else {
            onRequestPermissions()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("WiFi & BLE Tester") },
                actions = {
                    val bluetoothTint = if (connectionState.isConnected()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    IconButton(onClick = handleStartScan) {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = "Start Scan",
                            tint = bluetoothTint
                        )
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
                    hasPermissions = hasPermissions,
                    connectionState = connectionState,
                    deviceStatus = deviceStatus,
                    onStartScan = handleStartScan,
                    onStopScan = viewModel::stopScan,
                    onDisconnect = viewModel::disconnect,
                    onRequestPermissions = onRequestPermissions
                )
            }

            item {
                Bt311DiscoverySection(
                    isScanning = uiState.isScanning,
                    targetDevice = uiState.devices.firstOrNull(),
                    onStartScan = handleStartScan,
                    onConnect = viewModel::connect
                )
            }

            item {
                WifiConfigSection(
                    isSending = uiState.isSendingWifi,
                    configs = uiState.wifiConfigs,
                    lastReportedName = uiState.gadgetWifiName,
                    onSend = viewModel::sendWifiConfig
                )
            }

            item {
                NetworkToolsSection(
                    gadgetWifiName = uiState.gadgetWifiName,
                    phoneWifiName = uiState.phoneWifiName,
                    networkMatch = uiState.wifiNetworkMatch,
                    autoDeviceIp = uiState.deviceIp,
                    isQuerying = uiState.isQueryingNetwork,
                    onQueryNetwork = viewModel::queryNetworkInfo
                )
            }

            item {
                WebConsoleSection(
                    deviceIp = uiState.deviceIp,
                    devicePort = uiState.devicePort,
                    webConsoleUrl = uiState.webConsoleUrl,
                    errorMessage = uiState.webConsoleError,
                    onIpChanged = viewModel::onDeviceIpChanged,
                    onPortChanged = viewModel::onDevicePortChanged,
                    onOpenConsole = handleOpenWebConsole,
                    onCloseConsole = viewModel::closeWebConsole
                )
            }

            item {
                BleTrafficLogSection(entries = uiState.transportLog)
            }
        }
    }
}
@Composable
private fun ConnectionStatusCard(
    hasPermissions: Boolean,
    connectionState: BleConnectionState,
    deviceStatus: DeviceStatus?,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onDisconnect: () -> Unit,
    onRequestPermissions: () -> Unit
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

            if (!hasPermissions) {
                AssistChip(
                    onClick = onRequestPermissions,
                    label = { Text("授予蓝牙权限") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Bluetooth,
                            contentDescription = null
                        )
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onStartScan,
                    enabled = hasPermissions
                ) {
                    Text("开始扫描")
                }
                OutlinedButton(onClick = onStopScan) {
                    Text("停止扫描")
                }
                Button(
                    onClick = onDisconnect,
                    enabled = connectionState.canDisconnect()
                ) {
                    Text("停止连接")
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
private fun Bt311DiscoverySection(
    isScanning: Boolean,
    targetDevice: BluetoothDevice?,
    onStartScan: () -> Unit,
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
                text = "BT311 扫描状态",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val statusText = when {
                targetDevice != null -> "BT311 已找到，可立即连接。"
                isScanning -> "Searching..."
                else -> "点击“开始扫描”以寻找目标设备。"
            }
            Text(statusText)

            when {
                targetDevice != null -> {
                    Text(
                        text = targetDevice.address,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = { onConnect(targetDevice) }) {
                        Text("连接 BT311")
                    }
                }
                isScanning -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Searching")
                    }
                }
                else -> {
                    OutlinedButton(onClick = onStartScan) {
                        Text("重新扫描")
                    }
                }
            }

            Text(
                text = "扫描会自动重试，直到手动停止或已连接 BT311。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
@Composable
private fun WifiConfigSection(
    isSending: Boolean,
    configs: List<SavedWifiConfig>,
    lastReportedName: String?,
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
                text = "WiFi 名称配置 (wifi#connect#name#password)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            var wifiName by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = wifiName,
                onValueChange = { wifiName = it },
                label = { Text("WiFi 名称") },
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
                    if (wifiName.isNotBlank()) {
                        onSend(wifiName, password)
                    }
                },
                enabled = !isSending && wifiName.isNotBlank()
            ) {
                Text(if (isSending) "发送中..." else "发送到设备")
            }

            lastReportedName?.let {
                Text(
                    text = "设备当前 WiFi 名称: $it",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (configs.isNotEmpty()) {
                Divider()
                Text(
                    text = "已保存的 WiFi 名称",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    configs.forEach { config ->
                        AssistChip(
                            onClick = { onSend(config.wifiName, config.password) },
                            label = { Text(config.wifiName) },
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
                    text = "暂未保存任何 WiFi 配置。",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun WebConsoleSection(
    deviceIp: String,
    devicePort: String,
    webConsoleUrl: String?,
    errorMessage: String?,
    onIpChanged: (String) -> Unit,
    onPortChanged: (String) -> Unit,
    onOpenConsole: () -> Unit,
    onCloseConsole: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "设备 Web 控制台",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = deviceIp,
                onValueChange = onIpChanged,
                label = { Text("设备 IP 地址") },
                singleLine = true,
                placeholder = { Text("例如 192.168.0.109") }
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
                    onClick = onOpenConsole,
                    enabled = deviceIp.isNotBlank()
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("打开全屏 Web 控制台")
                }
                if (webConsoleUrl != null) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onCloseConsole
                    ) {
                        Text("清除链接")
                    }
                }
            }

            if (webConsoleUrl != null) {
                Text(
                    text = "最近访问: $webConsoleUrl",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

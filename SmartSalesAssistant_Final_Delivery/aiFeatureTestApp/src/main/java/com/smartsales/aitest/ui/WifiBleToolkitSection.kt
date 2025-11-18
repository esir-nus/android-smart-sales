package com.smartsales.aitest.ui

import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.wifibletest.ui.WifiBleTestUiState

@Composable
fun WifiBleToolkitSection(
    state: WifiBleTestUiState,
    connectionState: BleConnectionState,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
    onSendWifi: (String, String) -> Unit,
    onDeviceIpChange: (String) -> Unit,
    onDevicePortChange: (String) -> Unit,
    onQueryNetwork: () -> Unit,
    onOpenConsole: () -> String?,
    onOpenMediaServer: () -> String?,
    onCloseConsole: () -> Unit,
) {
    val context = LocalContext.current
    var wifiName by rememberSaveable { mutableStateOf("") }
    var wifiPassword by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.wifiConfigs) {
        if (wifiName.isBlank()) {
            state.wifiConfigs.firstOrDefault()?.let { config ->
                wifiName = config.wifiName
                wifiPassword = config.password
            }
        }
    }

    Card {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "WiFi / BLE 联调工具",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "用于 BT311 入网、网络自检与 Web 控制台调试。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ScanControls(
                state = state,
                connectionState = connectionState,
                hasPermissions = hasPermissions,
                onRequestPermissions = onRequestPermissions,
                onStartScan = onStartScan,
                onStopScan = onStopScan,
            )

            DeviceList(
                devices = state.devices,
                connectionState = connectionState,
                onConnect = onConnect,
                onDisconnect = onDisconnect,
            )

            WifiConfigForm(
                state = state,
                wifiName = wifiName,
                wifiPassword = wifiPassword,
                onWifiNameChange = { wifiName = it },
                onWifiPasswordChange = { wifiPassword = it },
                onSendWifi = onSendWifi,
            )

            NetworkTools(
                state = state,
                onDeviceIpChange = onDeviceIpChange,
                onDevicePortChange = onDevicePortChange,
                onQueryNetwork = onQueryNetwork,
                onOpenConsole = {
                    onOpenConsole()?.let { openExternalUrl(context, it) }
                },
                onOpenMediaServer = {
                    onOpenMediaServer()?.let { openExternalUrl(context, it) }
                },
                onCloseConsole = onCloseConsole,
            )

            TransportLog(state = state)
        }
    }
}

@Composable
private fun ScanControls(
    state: WifiBleTestUiState,
    connectionState: BleConnectionState,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Bluetooth, contentDescription = null)
            val statusText =
                when (connectionState) {
                    is BleConnectionState.Ready -> "已连接 ${connectionState.device.name ?: connectionState.device.address}"
                    is BleConnectionState.Connecting -> "连接中..."
                    is BleConnectionState.Scanning -> "扫描中..."
                    else -> "待连接"
                }
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    if (hasPermissions) {
                        onStartScan()
                    } else {
                        onRequestPermissions()
                    }
                },
            ) {
                Text(if (state.isScanning) "扫描中..." else "扫描设备")
            }
            if (state.isScanning) {
                OutlinedButton(onClick = onStopScan) {
                    Text("停止扫描")
                }
            }
        }
        if (!hasPermissions) {
            Text(
                text = "请授予蓝牙/定位权限后再尝试扫描。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDevice>,
    connectionState: BleConnectionState,
    onConnect: (BluetoothDevice) -> Unit,
    onDisconnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "附近设备",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        if (devices.isEmpty()) {
            Text(
                text = "未发现 BT311 广播，可稍后重试。",
                style = MaterialTheme.typography.bodySmall,
            )
            return
        }
        val connectedDevice = (connectionState as? BleConnectionState.Ready)?.device
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(devices) { device ->
                DeviceRow(
                    device = device,
                    isConnected = connectedDevice?.address == device.address,
                    onConnect = { onConnect(device) },
                    onDisconnect = onDisconnect,
                )
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: BluetoothDevice,
    isConnected: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name ?: "未知设备",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = device.address ?: "N/A",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isConnected) {
            TextButton(onClick = onDisconnect) {
                Text("断开")
            }
        } else {
            Button(onClick = onConnect) {
                Text("连接")
            }
        }
    }
}

@Composable
private fun WifiConfigForm(
    state: WifiBleTestUiState,
    wifiName: String,
    wifiPassword: String,
    onWifiNameChange: (String) -> Unit,
    onWifiPasswordChange: (String) -> Unit,
    onSendWifi: (String, String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "WiFi 配置",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = wifiName,
            onValueChange = onWifiNameChange,
            label = { Text("WiFi 名称 (SSID)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = wifiPassword,
            onValueChange = onWifiPasswordChange,
            label = { Text("密码") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.wifiConfigs.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.wifiConfigs.take(3).forEach { config ->
                    AssistChip(
                        onClick = {
                            onWifiNameChange(config.wifiName)
                            onWifiPasswordChange(config.password)
                        },
                        label = { Text(config.wifiName) },
                        leadingIcon =
                            if (config.isDefault) {
                                {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                    )
                                }
                            } else {
                                null
                            },
                    )
                }
            }
        }
        Button(
            onClick = { onSendWifi(wifiName.trim(), wifiPassword) },
            enabled = wifiName.isNotBlank() && !state.isSendingWifi,
        ) {
            Text(if (state.isSendingWifi) "下发中..." else "下发 WiFi 配置")
        }
    }
}

@Composable
private fun NetworkTools(
    state: WifiBleTestUiState,
    onDeviceIpChange: (String) -> Unit,
    onDevicePortChange: (String) -> Unit,
    onQueryNetwork: () -> Unit,
    onOpenConsole: () -> Unit,
    onOpenMediaServer: () -> Unit,
    onCloseConsole: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "网络 & 控制台",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        OutlinedTextField(
            value = state.deviceIp,
            onValueChange = onDeviceIpChange,
            label = { Text("设备 IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.devicePort,
            onValueChange = onDevicePortChange,
            label = { Text("端口") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        val matchMessage =
            when (state.wifiNetworkMatch) {
                true -> "手机与设备在同一网络"
                false -> "网络不一致，请重新配网"
                null -> "等待设备回传网络信息"
            }
        val matchColor =
            when (state.wifiNetworkMatch) {
                true -> MaterialTheme.colorScheme.primary
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        Text(
            text = matchMessage,
            color = matchColor,
            style = MaterialTheme.typography.bodySmall,
        )
        if (!state.gadgetWifiName.isNullOrBlank()) {
            Text(
                text = "设备 WiFi：${state.gadgetWifiName}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (!state.phoneWifiName.isNullOrBlank()) {
            Text(
                text = "手机 WiFi：${state.phoneWifiName}",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onQueryNetwork, enabled = !state.isQueryingNetwork) {
                Text(if (state.isQueryingNetwork) "查询中..." else "查询设备网络")
            }
            OutlinedButton(onClick = onOpenConsole) {
                Text("打开 Web 控制台")
            }
            OutlinedButton(onClick = onOpenMediaServer) {
                Text("媒体服务器")
            }
        }

        state.webConsoleUrl?.let { url ->
            Text(
                text = "最近访问：$url",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onCloseConsole) {
                Text("清除链接")
            }
        }
    }
}

@Composable
private fun TransportLog(state: WifiBleTestUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "最近 BLE 数据",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        if (state.transportLog.isEmpty()) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.transportLog.take(5).forEach { entry ->
                Text(
                    text = "${entry.direction.name}: ${entry.textPreview}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun openExternalUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun List<com.smartsales.wifibletest.data.SavedWifiConfig>.firstOrDefault() =
    firstOrNull { it.isDefault } ?: firstOrNull()

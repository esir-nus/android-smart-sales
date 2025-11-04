package com.smartsales.ui.screens.device

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.ui.components.LoadingIndicator
import com.smartsales.ui.components.NoDevicesEmptyState

/**
 * Device Pairing Screen
 * 
 * Scan and pair with BLE devices
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    onNavigateBack: () -> Unit,
    viewModel: DevicePairingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    var showWifiDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设备配对") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Scan/Stop button
                    IconButton(
                        onClick = {
                            if (connectionState.isScanning()) {
                                viewModel.stopScan()
                            } else {
                                viewModel.startScan()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (connectionState.isScanning()) {
                                Icons.Default.Stop
                            } else {
                                Icons.Default.Search
                            },
                            contentDescription = if (connectionState.isScanning()) {
                                "停止扫描"
                            } else {
                                "扫描设备"
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Connection status card
            ConnectionStatusCard(
                connectionState = connectionState,
                onDisconnect = { viewModel.disconnect() },
                onConfigureWifi = {
                    showWifiDialog = true
                    selectedDevice = connectionState.getDevice()
                }
            )
            
            Divider()
            
            // Device list
            when {
                uiState.isScanning -> {
                    if (uiState.devices.isEmpty()) {
                        LoadingIndicator(
                            modifier = Modifier.fillMaxSize(),
                            message = "正在扫描设备..."
                        )
                    } else {
                        DeviceList(
                            devices = uiState.devices,
                            onDeviceClick = { device ->
                                viewModel.connectDevice(device)
                            }
                        )
                    }
                }
                
                uiState.devices.isEmpty() && !connectionState.isConnected() -> {
                    NoDevicesEmptyState(
                        onStartScan = { viewModel.startScan() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    DeviceList(
                        devices = uiState.devices,
                        onDeviceClick = { device ->
                            viewModel.connectDevice(device)
                        }
                    )
                }
            }
        }
    }
    
    // WiFi configuration dialog
    if (showWifiDialog && selectedDevice != null) {
        WifiConfigDialog(
            onDismiss = { showWifiDialog = false },
            onConfirm = { ssid, password ->
                viewModel.sendWifiConfig(ssid, password)
                showWifiDialog = false
            }
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: BleConnectionState,
    onDisconnect: () -> Unit,
    onConfigureWifi: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = when {
                        connectionState.isReady() -> MaterialTheme.colorScheme.primary
                        connectionState.isConnecting() -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {}
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = connectionState.getStatusMessage(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Action buttons when connected
            if (connectionState.isReady()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onConfigureWifi,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Wifi, "WiFi", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("配置WiFi")
                    }
                    
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, "断开", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("断开连接")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDevice>,
    onDeviceClick: (BluetoothDevice) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(devices) { device ->
            DeviceItem(
                device = device,
                onClick = { onDeviceClick(device) }
            )
        }
    }
}

@Composable
private fun DeviceItem(
    device: BluetoothDevice,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiConfigDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配置WiFi") },
        text = {
            Column {
                OutlinedTextField(
                    value = ssid,
                    onValueChange = { ssid = it },
                    label = { Text("WiFi名称 (SSID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("WiFi密码") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Default.Visibility
                                } else {
                                    Icons.Default.VisibilityOff
                                },
                                contentDescription = if (passwordVisible) "隐藏" else "显示"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(ssid, password) },
                enabled = ssid.isNotBlank() && password.isNotBlank()
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
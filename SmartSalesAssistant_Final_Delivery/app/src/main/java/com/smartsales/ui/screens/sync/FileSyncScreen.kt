package com.smartsales.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.data.network.model.GadgetFile
import com.smartsales.ui.components.LoadingIndicator
import com.smartsales.ui.components.NoFilesEmptyState

/**
 * File Sync Screen
 *
 * Sync files from device via WiFi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileSyncScreen(
    onNavigateBack: () -> Unit,
    viewModel: FileSyncViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<GadgetFile?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("文件同步") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { viewModel.refreshFileList() },
                        enabled = !uiState.isLoading,
                    ) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }

                    // Sync all button
                    if (uiState.files.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.syncAllFiles() },
                            enabled = !uiState.isSyncing,
                        ) {
                            Icon(Icons.Default.CloudDownload, "同步全部")
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Connection status
            ConnectionStatusBanner(
                isConnected = uiState.isConnected,
                onConnect = { viewModel.checkConnection() },
            )

            // Sync progress
            if (uiState.isSyncing) {
                SyncProgressCard(
                    currentFile = uiState.currentSyncFile,
                    progress = uiState.syncProgress,
                    filesCompleted = uiState.filesSynced,
                    totalFiles = uiState.totalFilesToSync,
                )
            }

            // File list
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        message = "正在加载文件列表...",
                    )
                }

                uiState.error != null -> {
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { viewModel.refreshFileList() },
                    )
                }

                uiState.files.isEmpty() -> {
                    NoFilesEmptyState(
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                else -> {
                    FileList(
                        files = uiState.files,
                        syncedFiles = uiState.syncedFileIds,
                        onDownloadClick = { file ->
                            viewModel.syncFile(file)
                        },
                        onDeleteClick = { file ->
                            fileToDelete = file
                            showDeleteDialog = true
                        },
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除文件") },
            text = { Text("确定要从设备上删除文件 \"${fileToDelete!!.name}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFile(fileToDelete!!)
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ConnectionStatusBanner(
    isConnected: Boolean,
    onConnect: () -> Unit,
) {
    if (!isConnected) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "设备未连接",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = "请确保设备已连接WiFi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                TextButton(onClick = onConnect) {
                    Text("重试")
                }
            }
        }
    }
}

@Composable
private fun SyncProgressCard(
    currentFile: String?,
    progress: Float,
    filesCompleted: Int,
    totalFiles: Int,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = "正在同步文件",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (currentFile != null) {
                Text(
                    text = currentFile,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$filesCompleted / $totalFiles 文件",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FileList(
    files: List<GadgetFile>,
    syncedFiles: Set<String>,
    onDownloadClick: (GadgetFile) -> Unit,
    onDeleteClick: (GadgetFile) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        items(
            items = files,
            key = { it.id },
        ) { file ->
            FileItem(
                file = file,
                isSynced = syncedFiles.contains(file.id),
                onDownloadClick = { onDownloadClick(file) },
                onDeleteClick = { onDeleteClick(file) },
            )
        }
    }
}

@Composable
private fun FileItem(
    file: GadgetFile,
    isSynced: Boolean,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // File icon
            Icon(
                imageVector =
                    when {
                        file.isAudio() -> Icons.Default.AudioFile
                        file.isImage() -> Icons.Default.Image
                        else -> Icons.Default.InsertDriveFile
                    },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )

            Spacer(modifier = Modifier.width(16.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = file.getFormattedSize(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (isSynced) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "已同步",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = "已同步",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Action buttons
            Row {
                if (!isSynced) {
                    IconButton(onClick = onDownloadClick) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "下载",
                        )
                    }
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    Divider()
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

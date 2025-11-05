package com.smartsales.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Empty State Component
 * 
 * Shows when there's no data to display
 */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        icon?.invoke()
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Message
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        // Action button
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actionButton()
        }
    }
}

/**
 * No Conversations Empty State
 */
@Composable
fun NoConversationsEmptyState(
    onCreateConversation: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        message = "还没有对话记录\n点击下方按钮开始新对话",
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        actionButton = {
            Button(onClick = onCreateConversation) {
                Text("开始新对话")
            }
        }
    )
}

/**
 * No Devices Empty State
 */
@Composable
fun NoDevicesEmptyState(
    onStartScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyState(
        message = "未发现设备\n请确保设备已开启并在附近",
        modifier = modifier,
        actionButton = {
            Button(onClick = onStartScan) {
                Text("开始扫描")
            }
        }
    )
}

/**
 * No Files Empty State
 */
@Composable
fun NoFilesEmptyState(
    modifier: Modifier = Modifier
) {
    EmptyState(
        message = "设备上没有待同步的文件",
        modifier = modifier
    )
}

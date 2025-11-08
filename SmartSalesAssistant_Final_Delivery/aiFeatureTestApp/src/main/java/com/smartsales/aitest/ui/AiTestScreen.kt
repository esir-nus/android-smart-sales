package com.smartsales.aitest.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTestScreen(
    viewModel: AiTestViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.chatError, state.tingwuError) {
        when {
            state.chatError != null -> {
                snackbarHostState.showSnackbar(state.chatError!!, duration = SnackbarDuration.Long)
                viewModel.clearNotifications()
            }
            state.tingwuError != null -> {
                snackbarHostState.showSnackbar(state.tingwuError!!, duration = SnackbarDuration.Long)
                viewModel.clearNotifications()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AI 功能测试") })
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ChatSection(
                state = state,
                onSystemPromptChange = viewModel::updateSystemPrompt,
                onChatInputChange = viewModel::updateChatInput,
                onSendMessage = viewModel::sendChatMessage,
                onResetChat = viewModel::resetChat
            )

            TingwuSection(
                state = state,
                onAudioUrlChange = viewModel::updateAudioUrl,
                onSpeakerCountChange = viewModel::updateSpeakerCount,
                onToggleDiarization = viewModel::toggleDiarization,
                onStartTranscription = viewModel::startTranscription,
                onReset = viewModel::resetTranscription
            )

            ReferenceSection()
        }
    }
}

@Composable
private fun ChatSection(
    state: AiTestUiState,
    onSystemPromptChange: (String) -> Unit,
    onChatInputChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onResetChat: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Qwen 聊天测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.systemPrompt,
                onValueChange = onSystemPromptChange,
                label = { Text("系统提示词") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 260.dp)
                    .verticalScroll(scrollState)
            ) {
                if (state.chatMessages.isEmpty()) {
                    Text(
                        text = "暂无对话，输入内容开始聊天。",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.chatMessages.forEach { message ->
                            ChatMessageBubble(message = message)
                        }
                    }
                }
            }

            if (state.isChatLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(
                value = state.chatInput,
                onValueChange = onChatInputChange,
                label = { Text("发送消息") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onSendMessage,
                    enabled = !state.isChatLoading && state.chatInput.isNotBlank()
                ) {
                    Text("发送")
                }
                TextButton(onClick = onResetChat) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("清空会话")
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = if (isUser) "我" else "Dashscope",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TingwuSection(
    state: AiTestUiState,
    onAudioUrlChange: (String) -> Unit,
    onSpeakerCountChange: (String) -> Unit,
    onToggleDiarization: (Boolean) -> Unit,
    onStartTranscription: () -> Unit,
    onReset: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tingwu 转写测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = state.tingwuAudioUrl,
                onValueChange = onAudioUrlChange,
                label = { Text("音频文件 URL") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.tingwuSpeakerCount,
                    onValueChange = onSpeakerCountChange,
                    label = { Text("说话人数量") },
                    modifier = Modifier.weight(1f)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("角色分离", style = MaterialTheme.typography.labelSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = state.diarizationEnabled,
                            onCheckedChange = onToggleDiarization,
                            colors = SwitchDefaults.colors()
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.diarizationEnabled) "已开启" else "已关闭")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStartTranscription,
                    enabled = !state.isTingwuProcessing && state.tingwuAudioUrl.isNotBlank()
                ) {
                    Text("开始转写")
                }
                OutlinedButton(onClick = onReset) {
                    Text("重置任务")
                }
            }

            if (state.isTingwuProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            state.tingwuStatusMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            state.tingwuTaskId?.let {
                Text("任务 ID: $it", style = MaterialTheme.typography.bodySmall)
            }

            state.tingwuResultText?.let { result ->
                HorizontalDivider()
                Text("转写结果", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = result,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceSection() {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "使用提示",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "1. 请在 local.properties 中配置 DASHSCOPE_API_KEY 与 TINGWU_API_KEY。",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "2. 音频 URL 需可直接访问，建议使用 5 分钟以内的 MP3 文件。",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "3. 参考仓库 sample_code.md 获取更多阿里云调用示例。",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

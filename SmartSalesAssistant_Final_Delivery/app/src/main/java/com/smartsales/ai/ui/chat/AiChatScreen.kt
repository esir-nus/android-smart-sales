package com.smartsales.ai.ui.chat

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.ai.chat.AiAttachment
import com.smartsales.ai.chat.AiMessage
import com.smartsales.ai.chat.AiTaskMode
import com.smartsales.ai.ui.AiRoutes

@Composable
fun AiChatRoute(
    sessionId: String,
    onOpenMenu: () -> Unit,
    onNavigateStructured: (String) -> Unit,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mimeTypes =
        remember {
            arrayOf(
                "image/png",
                "image/jpeg",
                "image/webp",
                "audio/mpeg",
            )
        }

    val uploadLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                try {
                    if (uri.scheme == "content") {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                    }
                } catch (_: SecurityException) {
                    // Some providers do not support persistable permissions; best-effort only.
                }
                viewModel.importAttachment(uri.toString())
            }
        }

    LaunchedEffect(sessionId) {
        if (sessionId == AiRoutes.NEW_SESSION_ID) {
            viewModel.startNewChat()
        } else {
            viewModel.loadSession(sessionId)
        }
    }

    LaunchedEffect(state.pendingStructuredSessionId) {
        val sessionId = state.pendingStructuredSessionId ?: return@LaunchedEffect
        onNavigateStructured(sessionId)
        viewModel.onStructuredNavigationHandled()
    }

    AiChatScreen(
        state = state,
        onPromptChange = viewModel::onPromptChange,
        onSendClick = viewModel::sendMessage,
        onModeSelected = viewModel::selectMode,
        onAttachmentRemove = viewModel::removeAttachment,
        onUploadClick = { uploadLauncher.launch(mimeTypes) },
        onOpenMenu = onOpenMenu,
        onStartNewChat = viewModel::startNewChat,
    )
}

@Composable
fun AiChatScreen(
    state: AiChatUiState,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onModeSelected: (AiTaskMode) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onUploadClick: () -> Unit,
    onOpenMenu: () -> Unit,
    onStartNewChat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AiChatTopBar(
                title = state.title,
                onMenuClick = onOpenMenu,
                onNewChatClick = onStartNewChat,
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                reverseLayout = false,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.messages.isEmpty()) {
                    item {
                        AiChatGreetingCard()
                    }
                }

                items(state.messages) { message ->
                    when (message) {
                        is AiMessage.User -> UserBubble(message)
                        is AiMessage.Assistant -> AssistantBubble(message)
                        is AiMessage.System -> SystemBubble(message)
                    }
                }

                state.liveResponseMarkdown?.takeIf { state.isStreaming }?.let { streaming ->
                    item {
                        AssistantStreamingBubble(streaming)
                    }
                }
            }

            AiChatInputArea(
                prompt = state.prompt,
                selectedMode = state.selectedMode,
                attachments = state.attachments,
                onPromptChange = onPromptChange,
                onSendClick = onSendClick,
                onModeSelected = onModeSelected,
                onAttachmentRemove = onAttachmentRemove,
                onUploadClick = onUploadClick,
                isStreaming = state.isStreaming,
                modifier = Modifier.navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun AiChatTopBar(
    title: String,
    onMenuClick: () -> Unit,
    onNewChatClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Outlined.Menu, contentDescription = "菜单")
            }
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(text = "16:30  $title", style = MaterialTheme.typography.titleSmall)
                Text(text = "AI 助手", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        IconButton(onClick = onNewChatClick) {
            Icon(Icons.Outlined.Add, contentDescription = "新对话")
        }
    }
}

@Composable
private fun AiChatGreetingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Hi XXX", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "我是你的销售助手")
            Text(text = "我可以为你：")
            Text(" - 分析用户画像、渠道、痛点")
            Text(" - 生成 PDF、CSV 文档、思维导图")
            Text("开始我们的任务吧", modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun UserBubble(message: AiMessage.User) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        if (message.attachments.isNotEmpty()) {
            AttachmentRow(
                attachments = message.attachments,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun AssistantBubble(message: AiMessage.Assistant) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.92f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = message.markdown)
            if (message.exportTargets.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    message.exportTargets.forEach { target ->
                        OutlinedButton(onClick = { /* Hooked later */ }) {
                            Text(text = target.name)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemBubble(message: AiMessage.System) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = message.instructions,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AssistantStreamingBubble(markdown: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.92f),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(text = "正在生成…")
            }
            Text(text = markdown, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun AiChatInputArea(
    prompt: String,
    selectedMode: AiTaskMode,
    attachments: List<AiAttachment>,
    onPromptChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onModeSelected: (AiTaskMode) -> Unit,
    onAttachmentRemove: (String) -> Unit,
    onUploadClick: () -> Unit,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "上传本地文件并发送消息",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
        )
        SkillChipsRow(
            selectedMode = selectedMode,
            onModeSelected = onModeSelected,
        )
        AttachmentRow(
            attachments = attachments,
            onRemove = onAttachmentRemove,
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp),
            placeholder = { Text("输入内容…") },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onUploadClick) {
                Text("+ 上传")
            }
            OutlinedButton(
                enabled = !isStreaming,
                onClick = onSendClick,
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
private fun SkillChipsRow(
    selectedMode: AiTaskMode,
    onModeSelected: (AiTaskMode) -> Unit,
) {
    val modes =
        remember {
            listOf(
                AiTaskMode.AnalyzeCustomer,
                AiTaskMode.GeneratePdf,
                AiTaskMode.GenerateCsv,
            )
        }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { mode ->
            AssistChip(
                label = { Text(mode.displayName) },
                onClick = { onModeSelected(mode) },
                colors =
                    AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedMode == mode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = if (selectedMode == mode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
            )
        }
    }
}

@Composable
private fun AttachmentRow(
    attachments: List<AiAttachment>,
    onRemove: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEach { attachment ->
            AttachmentPreview(
                attachment = attachment,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun AttachmentPreview(
    attachment: AiAttachment,
    onRemove: ((String) -> Unit)?,
) {
    Box(
        modifier =
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = onRemove != null) { onRemove?.invoke(attachment.localPath) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = attachment.type.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

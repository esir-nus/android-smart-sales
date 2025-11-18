package com.smartsales.tingwutest

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 文件：tingwuTestApp/src/main/java/com/smartsales/tingwutest/TingwuTestActivity.kt
// 模块：:tingwuTestApp
// 说明：Compose 壳，集中调试 Tingwu HTTP API
// 作者：创建于 2025-11-17
@AndroidEntryPoint
class TingwuTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: TingwuTestViewModel = hiltViewModel()
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    val audioPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        if (uri != null) {
                            scope.launch {
                                val cached = persistLocalAudio(context, uri)
                                if (cached != null) {
                                    viewModel.uploadLocalAudio(cached)
                                } else {
                                    viewModel.logMessage("无法读取音频文件")
                                }
                            }
                        }
                    }
                    TingwuTestScreen(
                        state = state,
                        onLanguageChange = viewModel::updateLanguage,
                        onTaskKeyChange = viewModel::updateTaskKey,
                        onRegenerateTaskKey = viewModel::regenerateTaskKey,
                        onPickLocalAudio = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                        onSubmit = viewModel::submitTask
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TingwuTestScreen(
    state: TingwuTestUiState,
    onLanguageChange: (String) -> Unit,
    onTaskKeyChange: (String) -> Unit,
    onRegenerateTaskKey: () -> Unit,
    onPickLocalAudio: () -> Unit,
    onSubmit: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Tingwu 离线调试") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "和官方 SDK 示例保持一致：使用 HTTP PUT /openapi/tingwu/v2/tasks，并遵循 ROA 签名。", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "官方文档：https://help.aliyun.com/zh/tingwu/api-tingwu-2023-09-30-createtask",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
                    .clickable { uriHandler.openUri(DOC_LINK) }
            )
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "官方推荐流程：先将本地音频上传到 OSS，使用可公开访问的 URL 再创建 Tingwu 任务。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onPickLocalAudio,
                        enabled = !state.isUploading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isUploading) {
                            LinearProgressIndicator(modifier = Modifier.width(80.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "上传中...")
                        } else {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "选择音频并上传 OSS")
                        }
                    }
                    UploadSummary(state = state, clipboard = clipboard)
                }
            }
            PipelineDiagnosticsCard(checks = state.pipelineChecks)
            DetailedDiagnosticsCard(entries = state.diagnosticLogs)
            StepTwoCard(
                state = state,
                onLanguageChange = onLanguageChange,
                onTaskKeyChange = onTaskKeyChange,
                onRegenerateTaskKey = onRegenerateTaskKey,
                onSubmit = onSubmit
            )
            Divider()
            Text(text = "任务状态", style = MaterialTheme.typography.titleMedium)
            state.jobs.forEach { job ->
                TingwuJobCard(job)
            }
            Divider()
            Text(text = "签名日志 (按时间倒序)", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    val content = state.logLines.joinToString("\n").ifBlank { "暂无签名日志" }
                    clipboard.setText(AnnotatedString(content))
                },
                enabled = state.logLines.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "复制签名日志")
            }
            Card {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (state.logLines.isEmpty()) {
                        Text(text = "暂无日志，提交任务后会显示签名/状态。")
                    } else {
                        state.logLines.forEach { line ->
                            Text(text = line, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PipelineDiagnosticsCard(checks: List<PipelineCheckpointUi>) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "运行诊断", style = MaterialTheme.typography.titleMedium)
            if (checks.isEmpty()) {
                Text(text = "暂无诊断信息", style = MaterialTheme.typography.bodySmall)
            } else {
                checks.forEachIndexed { index, check ->
                    PipelineCheckRow(check = check)
                    if (index < checks.lastIndex) {
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedDiagnosticsCard(entries: List<PipelineLogEntry>) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "详细日志", style = MaterialTheme.typography.titleMedium)
            if (entries.isEmpty()) {
                Text(text = "暂无诊断日志", style = MaterialTheme.typography.bodySmall)
            } else {
                entries.forEach { entry ->
                    DiagnosticRow(entry = entry)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(entry: PipelineLogEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.stage.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entry.level.displayName(),
                    color = diagnosticLevelColor(entry.level),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Text(
                text = entry.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(text = entry.message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PipelineCheckRow(check: PipelineCheckpointUi) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = check.title, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = check.status.displayLabel(),
                color = pipelineStatusColor(check.status),
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            text = check.message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun UploadSummary(state: TingwuTestUiState, clipboard: ClipboardManager) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (state.uploadedFileName == null) {
            Text(
                text = "尚未选择音频文件",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "当前音频：${state.uploadedFileName}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.lastUploadedObjectKey?.let {
            Text(
                text = "OSS Object：$it",
                style = MaterialTheme.typography.bodySmall
            )
        }
        state.uploadedFileUrl?.let { url ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { clipboard.setText(AnnotatedString(url)) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                }
            }
        }
    }
}

@Composable
private fun StepTwoCard(
    state: TingwuTestUiState,
    onLanguageChange: (String) -> Unit,
    onTaskKeyChange: (String) -> Unit,
    onRegenerateTaskKey: () -> Unit,
    onSubmit: () -> Unit
) {
    val readyToSubmit = state.uploadedFileUrl != null
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "步骤 2：提交 Tingwu 转写", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "系统会使用上一步自动生成的 OSS URL，无需手动填写。",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.sourceLanguage,
                    onValueChange = onLanguageChange,
                    label = { Text("SourceLanguage") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.taskKey,
                    onValueChange = onTaskKeyChange,
                    label = { Text("TaskKey") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onRegenerateTaskKey) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "重新生成")
                }
                Text(
                    text = "BaseUrl: ${state.baseUrl}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "AppKey(脱敏)：${state.appKeyMasked.ifBlank { "未配置" }}",
                style = MaterialTheme.typography.bodySmall
            )
            state.lastSubmittedJobId?.let {
                Text(
                    text = "最近任务：$it",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onSubmit,
                enabled = readyToSubmit && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (state.isSubmitting) "提交中..." else "提交转写任务")
            }
            if (!readyToSubmit) {
                Text(
                    text = "请先完成步骤 1 上传音频",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun PipelineStatus.displayLabel(): String = when (this) {
    PipelineStatus.IDLE -> "待开始"
    PipelineStatus.RUNNING -> "进行中"
    PipelineStatus.OK -> "正常"
    PipelineStatus.WARN -> "注意"
    PipelineStatus.ERROR -> "异常"
}

@Composable
private fun pipelineStatusColor(status: PipelineStatus): Color = when (status) {
    PipelineStatus.OK -> MaterialTheme.colorScheme.primary
    PipelineStatus.RUNNING -> MaterialTheme.colorScheme.tertiary
    PipelineStatus.WARN -> MaterialTheme.colorScheme.tertiary
    PipelineStatus.ERROR -> MaterialTheme.colorScheme.error
    PipelineStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun PipelineLogStage.displayName(): String = when (this) {
    PipelineLogStage.TINGWU_CREDENTIALS -> "Tingwu 凭据"
    PipelineLogStage.OSS_CREDENTIALS -> "OSS 凭据"
    PipelineLogStage.OSS_UPLOAD -> "OSS 上传"
    PipelineLogStage.TINGWU_SUBMISSION -> "Tingwu 提交"
    PipelineLogStage.JOB_OBSERVER -> "任务追踪"
    PipelineLogStage.DATA_VALIDATION -> "参数校验"
    PipelineLogStage.UI_EVENT -> "界面提醒"
}

private fun PipelineLogLevel.displayName(): String = when (this) {
    PipelineLogLevel.INFO -> "信息"
    PipelineLogLevel.SUCCESS -> "成功"
    PipelineLogLevel.WARNING -> "警告"
    PipelineLogLevel.ERROR -> "错误"
}

@Composable
private fun diagnosticLevelColor(level: PipelineLogLevel): Color = when (level) {
    PipelineLogLevel.SUCCESS -> MaterialTheme.colorScheme.primary
    PipelineLogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    PipelineLogLevel.WARNING -> MaterialTheme.colorScheme.tertiary
    PipelineLogLevel.ERROR -> MaterialTheme.colorScheme.error
}

@Composable
private fun TingwuJobCard(job: TingwuJobUi) {
    val clipboard = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = job.jobId, style = MaterialTheme.typography.bodyMedium)
            Text(text = job.status, style = MaterialTheme.typography.bodySmall)
            if (job.progress in 1..99) {
                LinearProgressIndicator(
                    progress = job.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            } else if (job.progress >= 100) {
                LinearProgressIndicator(
                    progress = 1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            job.statusDetail?.let {
                Text(
                    text = "TaskStatus：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (job.artifactLinks.isNotEmpty()) {
                Text(text = "结果链接", style = MaterialTheme.typography.titleSmall)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    job.artifactLinks.forEach { artifact ->
                        ArtifactLinkRow(artifact = artifact, clipboard = clipboard, uriHandler = uriHandler)
                    }
                }
            }
            job.transcriptPreview?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            job.transcriptMarkdown?.let { markdown ->
                Text(text = "转写内容", style = MaterialTheme.typography.titleSmall)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = markdown, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(markdown)) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "复制全文")
                }
            }
            job.errorMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }
            job.errorCode?.let {
                Text(
                    text = "错误码：$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun ArtifactLinkRow(
    artifact: TingwuArtifactUi,
    clipboard: ClipboardManager,
    uriHandler: UriHandler
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = artifact.title, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = artifact.url,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { clipboard.setText(AnnotatedString(artifact.url)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制链接")
            }
            OutlinedButton(onClick = { runCatching { uriHandler.openUri(artifact.url) } }) {
                Text(text = "打开")
            }
        }
    }
}

private const val DOC_LINK = "https://help.aliyun.com/zh/tingwu/api-tingwu-2023-09-30-createtask"

private suspend fun persistLocalAudio(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val extension = when (resolver.getType(uri).orEmpty()) {
        "audio/wav", "audio/x-wav" -> "wav"
        "audio/m4a" -> "m4a"
        "audio/mp4" -> "mp4"
        else -> "mp3"
    }
    val targetDir = File(context.cacheDir, "tingwu-local-audio").apply { mkdirs() }
    val targetFile = File(targetDir, "local-audio-${System.currentTimeMillis()}.$extension")
    resolver.openInputStream(uri)?.use { input ->
        FileOutputStream(targetFile).use { output ->
            input.copyTo(output)
        }
    } ?: return@withContext null
    targetFile
}

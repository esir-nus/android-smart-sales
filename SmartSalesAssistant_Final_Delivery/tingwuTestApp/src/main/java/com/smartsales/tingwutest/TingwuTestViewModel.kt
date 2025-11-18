package com.smartsales.tingwutest

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.core.util.Result
import com.smartsales.data.aicore.AiCoreException
import com.smartsales.data.aicore.OssCredentials
import com.smartsales.data.aicore.OssCredentialsProvider
import com.smartsales.data.aicore.OssUploadClient
import com.smartsales.data.aicore.OssUploadRequest
import com.smartsales.data.aicore.TingwuCoordinator
import com.smartsales.data.aicore.TingwuCredentials
import com.smartsales.data.aicore.TingwuCredentialsProvider
import com.smartsales.data.aicore.TingwuJobState
import com.smartsales.data.aicore.TingwuJobArtifacts
import com.smartsales.data.aicore.TingwuRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 文件：tingwuTestApp/src/main/java/com/smartsales/tingwutest/TingwuTestViewModel.kt
// 模块：:tingwuTestApp
// 说明：管理 Tingwu 离线转写测试状态
// 作者：创建于 2025-11-17
@HiltViewModel
class TingwuTestViewModel @Inject constructor(
    private val tingwuCoordinator: TingwuCoordinator,
    private val ossUploadClient: OssUploadClient,
    credentialsProvider: TingwuCredentialsProvider,
    ossCredentialsProvider: OssCredentialsProvider
) : ViewModel() {

    private val timestampFormatter = SimpleDateFormat("HH:mm:ss", Locale.CHINA)
    private val taskKeyFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
    private val jobCollectors = ConcurrentHashMap<String, Job>()
    private var lastSubmissionFingerprint: String? = null
    private var lastSubmissionAtMillis: Long = 0L
    private val activeFingerprints = ConcurrentHashMap<String, String>()

    private val tingwuCredentials = runCatching { credentialsProvider.obtain() }.getOrNull()
    private val ossCredentials = runCatching { ossCredentialsProvider.obtain() }.getOrNull()

    private val _uiState = MutableStateFlow(
        TingwuTestUiState(
            sourceLanguage = "zh-CN",
            taskKey = generateTaskKey(),
            baseUrl = tingwuCredentials?.baseUrl.orEmpty(),
            appKeyMasked = maskSecret(tingwuCredentials?.appKey.orEmpty()),
            pipelineChecks = createInitialPipelineChecks(
                tingwuCredentials = tingwuCredentials,
                ossCredentials = ossCredentials
            )
        )
    )
    val uiState: StateFlow<TingwuTestUiState> = _uiState.asStateFlow()

    init {
        if (tingwuCredentials != null) {
            appendLog("Tingwu 凭据加载完成：baseUrl=${tingwuCredentials.baseUrl}")
            recordDiagnostic(
                stage = PipelineLogStage.TINGWU_CREDENTIALS,
                level = PipelineLogLevel.SUCCESS,
                message = "已加载 Tingwu 凭据：baseUrl=${tingwuCredentials.baseUrl}"
            )
        } else {
            appendLog("Tingwu 凭据缺失，请检查 local.properties")
            recordDiagnostic(
                stage = PipelineLogStage.TINGWU_CREDENTIALS,
                level = PipelineLogLevel.ERROR,
                message = "缺少 Tingwu 凭据，请配置 TINGWU_*"
            )
        }
        if (ossCredentials != null) {
            appendLog("OSS 凭据加载完成：bucket=${ossCredentials.bucket}")
            recordDiagnostic(
                stage = PipelineLogStage.OSS_CREDENTIALS,
                level = PipelineLogLevel.SUCCESS,
                message = "已加载 OSS 凭据：bucket=${ossCredentials.bucket}"
            )
        } else {
            appendLog("OSS 凭据缺失，无法上传音频")
            recordDiagnostic(
                stage = PipelineLogStage.OSS_CREDENTIALS,
                level = PipelineLogLevel.ERROR,
                message = "缺少 OSS_ACCESS_KEY_* 或 BUCKET 配置"
            )
        }
    }

    fun updateLanguage(value: String) {
        _uiState.update { it.copy(sourceLanguage = value) }
    }

    fun updateTaskKey(value: String) {
        _uiState.update { it.copy(taskKey = value) }
    }

    fun regenerateTaskKey() {
        _uiState.update { it.copy(taskKey = generateTaskKey()) }
    }

    fun logMessage(message: String) {
        appendLog(message)
        recordDiagnostic(
            stage = PipelineLogStage.UI_EVENT,
            level = PipelineLogLevel.WARNING,
            message = message
        )
    }

    fun submitTask() {
        val current = _uiState.value
        val fileUrl = current.uploadedFileUrl
        if (fileUrl.isNullOrBlank()) {
            appendLog("请先上传音频，系统会自动生成 Tingwu 所需的 URL")
            recordDiagnostic(
                stage = PipelineLogStage.DATA_VALIDATION,
                level = PipelineLogLevel.WARNING,
                message = "未检测到可用音频，无法提交 Tingwu 请求"
            )
            updateCheckpoint(PipelineCheckpointType.OSS_UPLOAD) {
                it.copy(
                    status = PipelineStatus.ERROR,
                    message = "未检测到可用音频"
                )
            }
            return
        }
        val normalizedLang = current.sourceLanguage.ifBlank { "zh-CN" }
        val finalTaskKey = current.taskKey.ifBlank { generateTaskKey() }
        evaluateUploadedUrl(fileUrl)
        val fingerprint = "${fileUrl.trim()}|$normalizedLang"
        val now = System.currentTimeMillis()
        val activeJob = activeFingerprints[fingerprint]
        if (activeJob != null) {
            appendLog("已存在进行中的任务($activeJob)，忽略重复请求。")
            recordDiagnostic(
                stage = PipelineLogStage.TINGWU_SUBMISSION,
                level = PipelineLogLevel.WARNING,
                message = "检测到进行中的 job=$activeJob，忽略重复提交"
            )
            return
        }
        if (fingerprint == lastSubmissionFingerprint && now - lastSubmissionAtMillis < DEDUP_WINDOW_MS) {
            appendLog("检测到连续重复请求，已忽略。")
            recordDiagnostic(
                stage = PipelineLogStage.TINGWU_SUBMISSION,
                level = PipelineLogLevel.WARNING,
                message = "短时间内重复点击触发防抖，未再次提交"
            )
            return
        }
        lastSubmissionFingerprint = fingerprint
        lastSubmissionAtMillis = now
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            updateCheckpoint(PipelineCheckpointType.TINGWU_SUBMISSION) {
                it.copy(
                    status = PipelineStatus.RUNNING,
                    message = "提交中..."
                )
            }
            recordDiagnostic(
                stage = PipelineLogStage.TINGWU_SUBMISSION,
                level = PipelineLogLevel.INFO,
                message = "开始提交 Tingwu 任务，lang=$normalizedLang key=$finalTaskKey"
            )
            val assetName = current.lastUploadedObjectKey ?: finalTaskKey
            val request = TingwuRequest(
                audioAssetName = assetName,
                language = normalizedLang,
                fileUrl = fileUrl.trim(),
                ossObjectKey = current.lastUploadedObjectKey
            )
            when (val result = tingwuCoordinator.submit(request)) {
                is Result.Success -> {
                    val jobId = result.data
                    appendLog("已提交任务，jobId=$jobId")
                    activeFingerprints[fingerprint] = jobId
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            lastSubmittedJobId = jobId
                        ).withJobPlaceholder(jobId)
                    }
                    observeJob(jobId)
                    updateCheckpoint(PipelineCheckpointType.TINGWU_SUBMISSION) {
                        it.copy(
                            status = PipelineStatus.OK,
                            message = "最新任务：$jobId"
                        )
                    }
                    recordDiagnostic(
                        stage = PipelineLogStage.TINGWU_SUBMISSION,
                        level = PipelineLogLevel.SUCCESS,
                        message = "Tingwu 请求提交成功：jobId=$jobId"
                    )
                }
                is Result.Error -> {
                    val message = extractMessage(result.throwable)
                    appendLog("提交失败：$message")
                    _uiState.update { it.copy(isSubmitting = false) }
                    updateCheckpoint(PipelineCheckpointType.TINGWU_SUBMISSION) {
                        it.copy(
                            status = PipelineStatus.ERROR,
                            message = message
                        )
                    }
                    recordDiagnostic(
                        stage = PipelineLogStage.TINGWU_SUBMISSION,
                        level = PipelineLogLevel.ERROR,
                        message = "提交失败：$message"
                    )
                }
            }
        }
    }

    fun uploadLocalAudio(file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            appendLog("上传音频至 OSS：${file.name}")
            recordDiagnostic(
                stage = PipelineLogStage.OSS_UPLOAD,
                level = PipelineLogLevel.INFO,
                message = "开始上传 ${file.name} (${file.length()} bytes)"
            )
            updateCheckpoint(PipelineCheckpointType.OSS_UPLOAD) {
                it.copy(
                    status = PipelineStatus.RUNNING,
                    message = "上传中：${file.name}"
                )
            }
            when (val result = ossUploadClient.uploadAudio(OssUploadRequest(file))) {
                is Result.Success -> {
                    appendLog("OSS 上传成功：key=${result.data.objectKey}")
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            uploadedFileUrl = result.data.presignedUrl,
                            lastUploadedObjectKey = result.data.objectKey,
                            uploadedFileName = file.name,
                            taskKey = buildTaskKeyFromObject(result.data.objectKey)
                        )
                    }
                    recordDiagnostic(
                        stage = PipelineLogStage.OSS_UPLOAD,
                        level = PipelineLogLevel.SUCCESS,
                        message = "OSS 上传成功：key=${result.data.objectKey}"
                    )
                    evaluateUploadedUrl(result.data.presignedUrl)
                    updateCheckpoint(PipelineCheckpointType.OSS_UPLOAD) {
                        it.copy(
                            status = PipelineStatus.OK,
                            message = "已上传：${result.data.objectKey}"
                        )
                    }
                }
                is Result.Error -> {
                    appendLog("OSS 上传失败：${extractMessage(result.throwable)}")
                    _uiState.update { it.copy(isUploading = false) }
                    updateCheckpoint(PipelineCheckpointType.OSS_UPLOAD) {
                        it.copy(
                            status = PipelineStatus.ERROR,
                            message = extractMessage(result.throwable)
                        )
                    }
                    recordDiagnostic(
                        stage = PipelineLogStage.OSS_UPLOAD,
                        level = PipelineLogLevel.ERROR,
                        message = "OSS 上传失败：${extractMessage(result.throwable)}"
                    )
                }
            }
        }
    }

    private fun observeJob(jobId: String) {
        if (jobCollectors.containsKey(jobId)) return
        recordDiagnostic(
            stage = PipelineLogStage.JOB_OBSERVER,
            level = PipelineLogLevel.INFO,
            message = "开始监听 jobId=$jobId"
        )
        val job = viewModelScope.launch {
            tingwuCoordinator.observeJob(jobId).collect { jobState ->
                val ui = mapJobState(jobId, jobState)
                _uiState.update { state ->
                    state.copy(
                        jobs = listOf(ui) + state.jobs.filterNot { it.jobId == jobId }
                    )
                }
                when (jobState) {
                    is TingwuJobState.Completed -> {
                        clearFingerprint(jobId)
                        appendLog("$jobId 已完成，返回 ${jobState.transcriptMarkdown.length} 字符")
                        updateCheckpoint(PipelineCheckpointType.TINGWU_SUBMISSION) {
                            it.copy(
                                status = PipelineStatus.OK,
                                message = "完成：$jobId"
                            )
                        }
                        recordDiagnostic(
                            stage = PipelineLogStage.JOB_OBSERVER,
                            level = PipelineLogLevel.SUCCESS,
                            message = "$jobId 已完成，返回 ${jobState.transcriptMarkdown.length} 字符"
                        )
                    }
                    is TingwuJobState.Failed -> {
                        clearFingerprint(jobId)
                        appendLog("$jobId 失败：${jobState.reason}")
                        updateCheckpoint(PipelineCheckpointType.TINGWU_SUBMISSION) {
                            it.copy(
                                status = PipelineStatus.ERROR,
                                message = jobState.reason
                            )
                        }
                        recordDiagnostic(
                            stage = PipelineLogStage.JOB_OBSERVER,
                            level = PipelineLogLevel.ERROR,
                            message = "$jobId 失败：${jobState.reason}"
                        )
                    }
                    else -> Unit
                }
            }
        }
        jobCollectors[jobId] = job
    }

    private fun mapJobState(jobId: String, state: TingwuJobState): TingwuJobUi = when (state) {
        TingwuJobState.Idle -> TingwuJobUi(jobId, "等待返回", 0, null, null, null)
        is TingwuJobState.InProgress -> {
            val label = state.statusLabel?.takeIf { it.isNotBlank() } ?: "处理中"
            TingwuJobUi(
                jobId = jobId,
                status = "$label (${state.progressPercent}%)",
                progress = state.progressPercent,
                transcriptPreview = null,
                transcriptMarkdown = null,
                errorMessage = null,
                artifactLinks = state.artifacts.toUiLinks(),
                statusDetail = state.statusLabel
            )
        }
        is TingwuJobState.Completed -> {
            val preview = state.transcriptMarkdown.lineSequence().firstOrNull()?.take(140)
            val label = state.statusLabel?.takeIf { it.isNotBlank() } ?: "已完成"
            TingwuJobUi(
                jobId = jobId,
                status = label,
                progress = 100,
                transcriptPreview = preview,
                transcriptMarkdown = state.transcriptMarkdown,
                errorMessage = null,
                artifactLinks = state.artifacts.toUiLinks(),
                statusDetail = state.statusLabel
            )
        }
        is TingwuJobState.Failed -> TingwuJobUi(
            jobId = jobId,
            status = "失败",
            progress = 0,
            transcriptPreview = null,
            transcriptMarkdown = null,
            errorMessage = state.reason,
            artifactLinks = emptyList(),
            statusDetail = null,
            errorCode = state.errorCode
        )
    }

    private fun TingwuJobArtifacts?.toUiLinks(): List<TingwuArtifactUi> {
        if (this == null) return emptyList()
        val outputLinks = listOf(
            "转换 MP3" to outputMp3Path,
            "转换 MP4" to outputMp4Path,
            "缩略图" to outputThumbnailPath,
            "波形图" to outputSpectrumPath
        ).mapNotNull { (title, url) ->
            url?.takeIf { it.isNotBlank() }?.let { TingwuArtifactUi(title, it) }
        }
        val resultLinks = resultLinks.map { TingwuArtifactUi(it.label, it.url) }
        return outputLinks + resultLinks
    }

    private fun generateTaskKey(): String = "tingwu_${taskKeyFormatter.format(Date())}"
    private fun buildTaskKeyFromObject(objectKey: String): String {
        val sanitized = objectKey.substringAfterLast("/").substringBeforeLast(".")
        return "tingwu_${sanitized.ifBlank { taskKeyFormatter.format(Date()) }}"
    }

    private fun appendLog(message: String) {
        val stamp = timestampFormatter.format(Date())
        _uiState.update { state ->
            val updated = listOf("[$stamp] $message") + state.logLines
            state.copy(logLines = updated.take(MAX_LOG_LINES))
        }
    }

    private fun maskSecret(value: String): String =
        when {
            value.isBlank() -> ""
            value.length <= 4 -> "****"
            else -> value.take(2) + "***" + value.takeLast(2)
        }

    private fun extractMessage(error: Throwable): String = when (error) {
        is AiCoreException -> error.userFacingMessage
        else -> error.message ?: "未知错误"
    }

    private fun updateCheckpoint(
        type: PipelineCheckpointType,
        transform: (PipelineCheckpointUi) -> PipelineCheckpointUi
    ) {
        _uiState.update { state ->
            val updated = state.pipelineChecks.map { checkpoint ->
                if (checkpoint.type == type) transform(checkpoint) else checkpoint
            }
            state.copy(pipelineChecks = updated)
        }
    }

    private fun recordDiagnostic(
        stage: PipelineLogStage,
        level: PipelineLogLevel,
        message: String
    ) {
        val entry = PipelineLogEntry(
            timestamp = timestampFormatter.format(Date()),
            stage = stage,
            level = level,
            message = message
        )
        _uiState.update { state ->
            val updated = (listOf(entry) + state.diagnosticLogs).take(MAX_DIAGNOSTIC_LOGS)
            state.copy(diagnosticLogs = updated)
        }
    }

    private fun evaluateUploadedUrl(url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        if (uri == null) {
            recordDiagnostic(
                stage = PipelineLogStage.DATA_VALIDATION,
                level = PipelineLogLevel.WARNING,
                message = "无法解析 OSS URL，请人工核对：$url"
            )
            return
        }
        val expires = uri.getQueryParameter("Expires")?.toLongOrNull()
        val scheme = uri.scheme.orEmpty()
        if (scheme.equals("http", ignoreCase = true)) {
            recordDiagnostic(
                stage = PipelineLogStage.DATA_VALIDATION,
                level = PipelineLogLevel.WARNING,
                message = "OSS URL 使用 HTTP，若 Tingwu 需 HTTPS 请调整配置"
            )
        }
        if (expires != null) {
            val secondsRemaining = expires - System.currentTimeMillis() / 1000
            when {
                secondsRemaining <= 0 -> recordDiagnostic(
                    stage = PipelineLogStage.DATA_VALIDATION,
                    level = PipelineLogLevel.ERROR,
                    message = "OSS URL 已过期，请重新上传"
                )
                secondsRemaining < 300 -> recordDiagnostic(
                    stage = PipelineLogStage.DATA_VALIDATION,
                    level = PipelineLogLevel.WARNING,
                    message = "OSS URL 将在 ${formatDuration(secondsRemaining)} 后过期"
                )
                else -> recordDiagnostic(
                    stage = PipelineLogStage.DATA_VALIDATION,
                    level = PipelineLogLevel.INFO,
                    message = "OSS URL 有效，剩余 ${formatDuration(secondsRemaining)}"
                )
            }
        } else {
            recordDiagnostic(
                stage = PipelineLogStage.DATA_VALIDATION,
                level = PipelineLogLevel.INFO,
                message = "OSS URL 未携带 Expires 参数，默认依赖 Bucket 策略"
            )
        }
    }

    private fun formatDuration(seconds: Long): String {
        val safeSeconds = seconds.coerceAtLeast(0)
        val minutes = safeSeconds / 60
        val remain = safeSeconds % 60
        return if (minutes > 0) {
            "${minutes}分${remain}秒"
        } else {
            "${remain}秒"
        }
    }

    private fun TingwuTestUiState.withJobPlaceholder(jobId: String): TingwuTestUiState {
        if (jobs.any { it.jobId == jobId }) return this
        val placeholder = TingwuJobUi(jobId, "等待返回", 0, null, null, null)
        return copy(jobs = listOf(placeholder) + jobs)
    }

    private fun clearFingerprint(jobId: String) {
        activeFingerprints.entries.removeIf { it.value == jobId }
    }

    companion object {
        private const val MAX_LOG_LINES = 40
        private const val MAX_DIAGNOSTIC_LOGS = 50
        private const val DEDUP_WINDOW_MS = 3_000L
    }
}

data class TingwuTestUiState(
    val sourceLanguage: String,
    val taskKey: String,
    val isSubmitting: Boolean = false,
    val isUploading: Boolean = false,
    val lastSubmittedJobId: String? = null,
    val baseUrl: String = "",
    val appKeyMasked: String = "",
    val uploadedFileName: String? = null,
    val uploadedFileUrl: String? = null,
    val lastUploadedObjectKey: String? = null,
    val jobs: List<TingwuJobUi> = emptyList(),
    val logLines: List<String> = emptyList(),
    val pipelineChecks: List<PipelineCheckpointUi> = emptyList(),
    val diagnosticLogs: List<PipelineLogEntry> = emptyList()
)

data class TingwuJobUi(
    val jobId: String,
    val status: String,
    val progress: Int,
    val transcriptPreview: String?,
    val transcriptMarkdown: String?,
    val errorMessage: String?,
    val artifactLinks: List<TingwuArtifactUi> = emptyList(),
    val statusDetail: String? = null,
    val errorCode: String? = null
)

data class TingwuArtifactUi(
    val title: String,
    val url: String
)

data class PipelineLogEntry(
    val timestamp: String,
    val stage: PipelineLogStage,
    val level: PipelineLogLevel,
    val message: String
)

enum class PipelineLogStage {
    TINGWU_CREDENTIALS,
    OSS_CREDENTIALS,
    OSS_UPLOAD,
    TINGWU_SUBMISSION,
    JOB_OBSERVER,
    DATA_VALIDATION,
    UI_EVENT
}

enum class PipelineLogLevel {
    INFO,
    SUCCESS,
    WARNING,
    ERROR
}

data class PipelineCheckpointUi(
    val type: PipelineCheckpointType,
    val title: String,
    val message: String,
    val status: PipelineStatus
)

enum class PipelineCheckpointType {
    TINGWU_CREDENTIALS,
    OSS_CREDENTIALS,
    OSS_UPLOAD,
    TINGWU_SUBMISSION
}

enum class PipelineStatus {
    IDLE,
    RUNNING,
    OK,
    WARN,
    ERROR
}

private fun createInitialPipelineChecks(
    tingwuCredentials: TingwuCredentials?,
    ossCredentials: OssCredentials?
): List<PipelineCheckpointUi> {
    val tingwuCheck = PipelineCheckpointUi(
        type = PipelineCheckpointType.TINGWU_CREDENTIALS,
        title = "Tingwu 凭据",
        message = tingwuCredentials?.baseUrl?.let { "BaseUrl: $it" } ?: "缺少密钥",
        status = if (tingwuCredentials == null) PipelineStatus.ERROR else PipelineStatus.OK
    )
    val ossCheck = PipelineCheckpointUi(
        type = PipelineCheckpointType.OSS_CREDENTIALS,
        title = "OSS 凭据",
        message = ossCredentials?.bucket?.let { "Bucket: $it" } ?: "缺少 OSS 配置",
        status = if (ossCredentials == null) PipelineStatus.ERROR else PipelineStatus.OK
    )
    val uploadCheck = PipelineCheckpointUi(
        type = PipelineCheckpointType.OSS_UPLOAD,
        title = "音频上传",
        message = "等待选择音频",
        status = PipelineStatus.IDLE
    )
    val taskCheck = PipelineCheckpointUi(
        type = PipelineCheckpointType.TINGWU_SUBMISSION,
        title = "Tingwu 任务",
        message = "等待提交",
        status = PipelineStatus.IDLE
    )
    return listOf(tingwuCheck, ossCheck, uploadCheck, taskCheck)
}

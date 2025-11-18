package com.smartsales.aitest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.network.AiApiConfig
import com.smartsales.data.network.api.TingwuApi
import com.smartsales.data.network.api.TingwuApiHelper
import com.smartsales.data.network.dashscope.DashscopeChatClient
import com.smartsales.data.network.dashscope.DashscopeChatPayload
import com.smartsales.data.network.dashscope.DashscopeStreamEvent
import com.smartsales.data.network.dashscope.MockDashscopeApiHelper
import com.smartsales.data.network.model.ChatInput
import com.smartsales.data.network.model.ChatMessage
import com.smartsales.data.network.model.ChatParameters
import com.smartsales.data.network.model.TranscriptionParameters
import com.smartsales.data.network.model.TranscriptionRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ROLE_USER = "user"
private const val ROLE_ASSISTANT = "assistant"

private const val DEFAULT_SYSTEM_PROMPT =
    "你是智能销售助手，擅长总结对话、提取销售要点，并提供后续行动建议。"

// Placeholder URL for documentation purposes; replace with a real, reachable audio asset when testing.
private const val DEFAULT_AUDIO_URL =
    "https://example.com/audio/sample.mp3"

data class AiChatMessage(
    val role: String,
    val content: String,
)

data class AiTestUiState(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val chatMessages: List<AiChatMessage> = emptyList(),
    val chatInput: String = "",
    val isChatLoading: Boolean = false,
    val chatError: String? = null,
    // Audio input options
    val audioInputMode: AudioInputMode = AudioInputMode.URL,
    val selectedAudioFile: java.io.File? = null,
    val tingwuAudioUrl: String = DEFAULT_AUDIO_URL,
    val tingwuSpeakerCount: String = "2",
    val diarizationEnabled: Boolean = true,
    // Transcription results
    val tingwuTaskId: String? = null,
    val tingwuStatusMessage: String? = null,
    val tingwuProgress: Int = 0,
    val tingwuResultText: String? = null,
    val isTingwuProcessing: Boolean = false,
    val tingwuError: String? = null,
    val rawTranscriptionData: com.smartsales.data.network.model.TranscriptionData? = null,
    // Analysis results
    val conversationSummary: String? = null,
    val customerAnalysis: String? = null,
    val mindmapAnalysis: String? = null,
    val isAnalysisLoading: Boolean = false,
    val analysisError: String? = null,
)

enum class AudioInputMode {
    URL,
    LOCAL_FILE,
}

@HiltViewModel
class AiTestViewModel
    @Inject
    constructor(
        private val dashscopeChatClient: DashscopeChatClient,
        private val tingwuApi: TingwuApi,
        private val ossManager: com.smartsales.data.network.oss.OssManager,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AiTestUiState())
        val uiState: StateFlow<AiTestUiState> = _uiState.asStateFlow()

        fun updateSystemPrompt(prompt: String) {
            _uiState.update { it.copy(systemPrompt = prompt) }
        }

        fun updateChatInput(text: String) {
            _uiState.update { it.copy(chatInput = text) }
        }

        fun sendChatMessage() {
            val message = _uiState.value.chatInput.trim()
            if (message.isEmpty()) {
                _uiState.update { it.copy(chatError = "请输入要发送的内容。") }
                return
            }

            val state = _uiState.value
            val updatedHistory = state.chatMessages + AiChatMessage(role = ROLE_USER, content = message)

            val chatMessages =
                buildList {
                    if (state.systemPrompt.isNotBlank()) {
                        add(ChatMessage.system(state.systemPrompt))
                    }
                    updatedHistory.forEach { historyMessage ->
                        add(ChatMessage(historyMessage.role, historyMessage.content))
                    }
                }

            _uiState.update {
                it.copy(
                    chatMessages = updatedHistory,
                    chatInput = "",
                    isChatLoading = true,
                    chatError = null,
                )
            }

            viewModelScope.launch {
                val payload =
                    DashscopeChatPayload(
                        model = AiApiConfig.Models.DEFAULT_CHAT_MODEL,
                        input = ChatInput(messages = chatMessages),
                        parameters = ChatParameters.streaming(),
                    )

                val replyBuilder = StringBuilder()
                var failure: Throwable? = null

                try {
                    dashscopeChatClient.streamChat(payload).collect { event ->
                        when (event) {
                            is DashscopeStreamEvent.Chunk -> replyBuilder.append(event.markdown)
                            is DashscopeStreamEvent.Completed -> Unit
                            is DashscopeStreamEvent.Error -> failure = event.throwable
                        }
                    }
                } catch (e: Exception) {
                    failure = e
                }

                if (failure != null) {
                    _uiState.update {
                        it.copy(
                            isChatLoading = false,
                            chatError = "网络问题，请稍后再试",
                        )
                    }
                } else {
                    val assistantReply =
                        if (replyBuilder.isNotEmpty()) {
                            replyBuilder.toString()
                        } else {
                            "（未返回内容）"
                        }
                    _uiState.update {
                        it.copy(
                            chatMessages =
                                it.chatMessages +
                                    AiChatMessage(
                                        role = ROLE_ASSISTANT,
                                        content = assistantReply,
                                    ),
                            isChatLoading = false,
                        )
                    }
                }
            }
        }

        fun updateAudioUrl(url: String) {
            _uiState.update { it.copy(tingwuAudioUrl = url, audioInputMode = AudioInputMode.URL) }
        }

        fun selectLocalAudioFile(file: java.io.File) {
            _uiState.update {
                it.copy(
                    selectedAudioFile = file,
                    audioInputMode = AudioInputMode.LOCAL_FILE,
                    tingwuAudioUrl = "file://${file.absolutePath}",
                )
            }
        }

        suspend fun uploadLocalAudioAndTranscribe(): Boolean {
            val state = _uiState.value
            val localFile = state.selectedAudioFile

            if (localFile == null || !localFile.exists()) {
                _uiState.update { it.copy(tingwuError = "请选择有效的音频文件") }
                return false
            }

            _uiState.update {
                it.copy(
                    isTingwuProcessing = true,
                    tingwuError = null,
                    tingwuStatusMessage = "正在上传音频到 OSS...",
                )
            }

            return try {
                // Upload to OSS
                val uploadResult = ossManager.uploadAudioFile(localFile)

                if (uploadResult.isSuccess) {
                    val publicUrl = uploadResult.getOrThrow()
                    _uiState.update {
                        it.copy(
                            tingwuAudioUrl = publicUrl,
                            tingwuStatusMessage = "上传完成，开始转写...",
                        )
                    }
                    // Now proceed with normal transcription
                    startTranscriptionWithUrl(publicUrl)
                    true
                } else {
                    val error = uploadResult.exceptionOrNull()?.message ?: "上传失败"
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = "网络问题，请稍后再试",
                        )
                    }
                    false
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTingwuProcessing = false,
                        tingwuError = "网络问题，请稍后再试",
                    )
                }
                false
            }
        }

        fun updateSpeakerCount(count: String) {
            _uiState.update { it.copy(tingwuSpeakerCount = count) }
        }

        fun resetChat() {
            _uiState.update {
                it.copy(
                    chatMessages = emptyList(),
                    chatError = null,
                    isChatLoading = false,
                )
            }
        }

        fun toggleDiarization(enabled: Boolean) {
            _uiState.update { it.copy(diarizationEnabled = enabled) }
        }

        fun startTranscription() {
            val state = _uiState.value

            // Handle local file upload if needed
            if (state.audioInputMode == AudioInputMode.LOCAL_FILE) {
                viewModelScope.launch {
                    uploadLocalAudioAndTranscribe()
                }
                return
            }

            // Handle URL-based transcription
            val fileUrl = state.tingwuAudioUrl.trim()
            if (fileUrl.isEmpty()) {
                _uiState.update { it.copy(tingwuError = "请提供可访问的音频文件 URL。") }
                return
            }

            startTranscriptionWithUrl(fileUrl)
        }

        private fun startTranscriptionWithUrl(fileUrl: String) {
            val state = _uiState.value
            val speakerCount = state.tingwuSpeakerCount.toIntOrNull()?.takeIf { it in 1..10 } ?: 2

            _uiState.update {
                it.copy(
                    isTingwuProcessing = true,
                    tingwuError = null,
                    tingwuStatusMessage = "正在创建 Tingwu 任务…",
                    tingwuResultText = null,
                    tingwuProgress = 0,
                )
            }

            viewModelScope.launch {
                try {
                    val request =
                        TranscriptionRequest(
                            parameters =
                                TranscriptionParameters(
                                    fileUrl = fileUrl,
                                    speakerCount = speakerCount,
                                    diarizationEnabled = state.diarizationEnabled,
                                    format = "json",
                                ),
                        )

                    val response = tingwuApi.createTranscriptionTask(request)
                    if (!response.isSuccessful) {
                        _uiState.update {
                            it.copy(
                                isTingwuProcessing = false,
                                tingwuError = "网络问题，请稍后再试",
                            )
                        }
                        return@launch
                    }

                    val taskId = response.body()?.taskId
                    if (taskId.isNullOrEmpty()) {
                        _uiState.update {
                            it.copy(
                                isTingwuProcessing = false,
                                tingwuError = "网络问题，请稍后再试",
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            tingwuTaskId = taskId,
                            tingwuStatusMessage = "任务已创建 (ID: $taskId)，开始轮询状态…",
                        )
                    }

                    val status =
                        TingwuApiHelper.pollTaskStatus(
                            taskId = taskId,
                            api = tingwuApi,
                            pollInterval = 5_000L,
                            timeout = 300_000L,
                        )

                    if (status == null) {
                        _uiState.update {
                            it.copy(
                                isTingwuProcessing = false,
                                tingwuError = "网络问题，请稍后再试",
                                tingwuStatusMessage = "网络超时",
                            )
                        }
                        return@launch
                    }

                    val statusLower = status.status.lowercase()
                    if (statusLower.contains("fail")) {
                        _uiState.update {
                            it.copy(
                                isTingwuProcessing = false,
                                tingwuError = "网络问题，请稍后再试",
                                tingwuStatusMessage = "处理失败",
                            )
                        }
                        return@launch
                    }

                    _uiState.update {
                        it.copy(
                            tingwuStatusMessage = "任务完成，正在获取转写结果…",
                            tingwuProgress = 100,
                        )
                    }

                    val resultResponse = tingwuApi.getTranscriptionResult(taskId)
                    if (resultResponse.isSuccessful) {
                        val result = resultResponse.body()
                        val transcriptionData = result?.transcription

                        val formatted =
                            transcriptionData?.let {
                                TingwuApiHelper.formatTranscriptionForDisplay(
                                    data = it,
                                    includeSpeaker = true,
                                    includeTimestamps = true,
                                )
                            } ?: "未返回转写内容。"

                        _uiState.update {
                            it.copy(
                                isTingwuProcessing = false,
                                tingwuResultText = formatted,
                                tingwuStatusMessage = "转写成功",
                                rawTranscriptionData = transcriptionData,
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isTingwuProcessing = false,
                                tingwuError = "网络问题，请稍后再试",
                                tingwuStatusMessage = "结果获取失败",
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = "网络问题，请稍后再试",
                            tingwuStatusMessage = "处理异常",
                        )
                    }
                }
            }
        }

        fun clearNotifications() {
            _uiState.update {
                it.copy(
                    chatError = null,
                    tingwuError = null,
                )
            }
        }

        fun resetTranscription() {
            _uiState.update {
                it.copy(
                    tingwuTaskId = null,
                    tingwuStatusMessage = null,
                    tingwuResultText = null,
                    tingwuProgress = 0,
                    tingwuError = null,
                    isTingwuProcessing = false,
                    rawTranscriptionData = null,
                    conversationSummary = null,
                    customerAnalysis = null,
                    mindmapAnalysis = null,
                )
            }
        }

        // Analysis Features
        fun generateConversationSummary() {
            val transcriptionData = _uiState.value.rawTranscriptionData ?: return

            _uiState.update {
                it.copy(
                    isAnalysisLoading = true,
                    analysisError = null,
                )
            }

            viewModelScope.launch {
                try {
                    val transcriptionText = transcriptionData.text
                    val prompt = MockDashscopeApiHelper.createSummaryPrompt(transcriptionText, maxLength = 200)

                    val messages =
                        buildList {
                            add(ChatMessage.system("你是一个专业的销售对话分析助手"))
                            add(ChatMessage.user(prompt))
                        }

                    val payload =
                        DashscopeChatPayload(
                            model = AiApiConfig.Models.DEFAULT_CHAT_MODEL,
                            input = ChatInput(messages = messages),
                            parameters = ChatParameters.default(),
                        )

                    val summaryBuilder = StringBuilder()
                    dashscopeChatClient.streamChat(payload).collect { event ->
                        when (event) {
                            is DashscopeStreamEvent.Chunk -> summaryBuilder.append(event.markdown)
                            is DashscopeStreamEvent.Error -> throw event.throwable
                            else -> Unit
                        }
                    }

                    _uiState.update {
                        it.copy(
                            conversationSummary = summaryBuilder.toString(),
                            isAnalysisLoading = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            analysisError = "生成摘要失败: ${e.message}",
                            isAnalysisLoading = false,
                        )
                    }
                }
            }
        }

        fun generateCustomerAnalysis() {
            val transcriptionData = _uiState.value.rawTranscriptionData ?: return

            _uiState.update {
                it.copy(
                    isAnalysisLoading = true,
                    analysisError = null,
                )
            }

            viewModelScope.launch {
                try {
                    val transcriptionText = transcriptionData.text
                    val prompt = MockDashscopeApiHelper.createCustomerAnalysisPrompt(transcriptionText)

                    val messages =
                        buildList {
                            add(ChatMessage.system("你是一个专业的客户信息提取助手，请返回JSON格式数据"))
                            add(ChatMessage.user(prompt))
                        }

                    val payload =
                        DashscopeChatPayload(
                            model = AiApiConfig.Models.DEFAULT_CHAT_MODEL,
                            input = ChatInput(messages = messages),
                            parameters = ChatParameters.default(),
                        )

                    val analysisBuilder = StringBuilder()
                    dashscopeChatClient.streamChat(payload).collect { event ->
                        when (event) {
                            is DashscopeStreamEvent.Chunk -> analysisBuilder.append(event.markdown)
                            is DashscopeStreamEvent.Error -> throw event.throwable
                            else -> Unit
                        }
                    }

                    _uiState.update {
                        it.copy(
                            customerAnalysis = analysisBuilder.toString(),
                            isAnalysisLoading = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            analysisError = "客户分析失败: ${e.message}",
                            isAnalysisLoading = false,
                        )
                    }
                }
            }
        }

        fun generateMindmapAnalysis() {
            val transcriptionData = _uiState.value.rawTranscriptionData ?: return

            _uiState.update {
                it.copy(
                    isAnalysisLoading = true,
                    analysisError = null,
                )
            }

            viewModelScope.launch {
                try {
                    val transcriptionText = transcriptionData.text
                    val prompt = createMindmapPrompt(transcriptionText)

                    val messages =
                        buildList {
                            add(ChatMessage.system("你是一个专业的思维导图生成助手"))
                            add(ChatMessage.user(prompt))
                        }

                    val payload =
                        DashscopeChatPayload(
                            model = AiApiConfig.Models.DEFAULT_CHAT_MODEL,
                            input = ChatInput(messages = messages),
                            parameters = ChatParameters.default(),
                        )

                    val mindmapBuilder = StringBuilder()
                    dashscopeChatClient.streamChat(payload).collect { event ->
                        when (event) {
                            is DashscopeStreamEvent.Chunk -> mindmapBuilder.append(event.markdown)
                            is DashscopeStreamEvent.Error -> throw event.throwable
                            else -> Unit
                        }
                    }

                    _uiState.update {
                        it.copy(
                            mindmapAnalysis = mindmapBuilder.toString(),
                            isAnalysisLoading = false,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            analysisError = "思维导图生成失败: ${e.message}",
                            isAnalysisLoading = false,
                        )
                    }
                }
            }
        }

        private fun createMindmapPrompt(transcription: String): String {
            return """
                请根据以下销售对话内容，生成一个结构化的思维导图，包含：
                
                对话内容：
                $transcription
                
                思维导图结构：
                1. 中心主题：销售对话核心
                2. 主要分支：
                   - 客户需求与痛点
                   - 产品/服务亮点  
                   - 异议处理
                   - 下一步行动
                3. 每个分支下的关键要点
                
                请以清晰的层级结构展示，使用缩进表示层级关系。
                """.trimIndent()
        }
    }

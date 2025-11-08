package com.smartsales.aitest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.network.AiApiConfig
import com.smartsales.data.network.api.DashscopeApi
import com.smartsales.data.network.api.TingwuApi
import com.smartsales.data.network.api.TingwuApiHelper
import com.smartsales.data.network.model.ChatInput
import com.smartsales.data.network.model.ChatMessage
import com.smartsales.data.network.model.ChatParameters
import com.smartsales.data.network.model.ChatRequest
import com.smartsales.data.network.model.TranscriptionParameters
import com.smartsales.data.network.model.TranscriptionRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

private const val ROLE_USER = "user"
private const val ROLE_ASSISTANT = "assistant"

private const val DEFAULT_SYSTEM_PROMPT =
    "你是智能销售助手，擅长总结对话、提取销售要点，并提供后续行动建议。"

// Placeholder URL for documentation purposes; replace with a real, reachable audio asset when testing.
private const val DEFAULT_AUDIO_URL =
    "https://example.com/audio/sample.mp3"

data class AiChatMessage(
    val role: String,
    val content: String
)

data class AiTestUiState(
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val chatMessages: List<AiChatMessage> = emptyList(),
    val chatInput: String = "",
    val isChatLoading: Boolean = false,
    val chatError: String? = null,

    val tingwuAudioUrl: String = DEFAULT_AUDIO_URL,
    val tingwuSpeakerCount: String = "2",
    val diarizationEnabled: Boolean = true,
    val tingwuTaskId: String? = null,
    val tingwuStatusMessage: String? = null,
    val tingwuProgress: Int = 0,
    val tingwuResultText: String? = null,
    val isTingwuProcessing: Boolean = false,
    val tingwuError: String? = null
)

@HiltViewModel
class AiTestViewModel @Inject constructor(
    private val dashScopeApi: DashscopeApi,
    private val tingwuApi: TingwuApi
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

        val chatMessages = buildList {
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
                chatError = null
            )
        }

        viewModelScope.launch {
            try {
                val request = ChatRequest(
                    model = AiApiConfig.Models.DEFAULT_CHAT_MODEL,
                    input = ChatInput(messages = chatMessages),
                    parameters = ChatParameters.default()
                )

                val response = dashScopeApi.chatCompletion(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    val assistantReply = body?.output?.choices?.firstOrNull()?.message?.content
                        ?: body?.output?.text
                        ?: "（未返回内容）"

                    _uiState.update {
                        it.copy(
                            chatMessages = it.chatMessages + AiChatMessage(
                                role = ROLE_ASSISTANT,
                                content = assistantReply
                            ),
                            isChatLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isChatLoading = false,
                            chatError = "聊天请求失败: ${response.code()} ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isChatLoading = false,
                        chatError = e.message ?: "聊天请求出现异常。"
                    )
                }
            }
        }
    }

    fun updateAudioUrl(url: String) {
        _uiState.update { it.copy(tingwuAudioUrl = url) }
    }

    fun updateSpeakerCount(count: String) {
        _uiState.update { it.copy(tingwuSpeakerCount = count) }
    }

    fun resetChat() {
        _uiState.update {
            it.copy(
                chatMessages = emptyList(),
                chatError = null,
                isChatLoading = false
            )
        }
    }

    fun toggleDiarization(enabled: Boolean) {
        _uiState.update { it.copy(diarizationEnabled = enabled) }
    }

    fun startTranscription() {
        val state = _uiState.value
        val fileUrl = state.tingwuAudioUrl.trim()

        if (fileUrl.isEmpty()) {
            _uiState.update { it.copy(tingwuError = "请提供可访问的音频文件 URL。") }
            return
        }

        val speakerCount = state.tingwuSpeakerCount.toIntOrNull()?.takeIf { it in 1..10 } ?: 2

        _uiState.update {
            it.copy(
                isTingwuProcessing = true,
                tingwuError = null,
                tingwuStatusMessage = "正在创建 Tingwu 任务…",
                tingwuResultText = null,
                tingwuProgress = 0
            )
        }

        viewModelScope.launch {
            try {
                val request = TranscriptionRequest(
                    parameters = TranscriptionParameters(
                        fileUrl = fileUrl,
                        speakerCount = speakerCount,
                        diarizationEnabled = state.diarizationEnabled,
                        format = "json"
                    )
                )

                val response = tingwuApi.createTranscriptionTask(request)
                if (!response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = "创建任务失败: ${response.code()} ${response.message()}"
                        )
                    }
                    return@launch
                }

                val taskId = response.body()?.taskId
                if (taskId.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = "未获取到任务 ID，请稍后重试。"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        tingwuTaskId = taskId,
                        tingwuStatusMessage = "任务已创建 (ID: $taskId)，开始轮询状态…"
                    )
                }

                val status = TingwuApiHelper.pollTaskStatus(
                    taskId = taskId,
                    api = tingwuApi,
                    pollInterval = 5_000L,
                    timeout = 300_000L
                )

                if (status == null) {
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = "轮询超时，请稍后重试。",
                            tingwuStatusMessage = "轮询超时"
                        )
                    }
                    return@launch
                }

                val statusLower = status.status.lowercase()
                if (statusLower.contains("fail")) {
                    val errorMessage = status.error?.message ?: "Tingwu 报告任务失败。"
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = errorMessage,
                            tingwuStatusMessage = "任务失败"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        tingwuStatusMessage = "任务完成，正在获取转写结果…",
                        tingwuProgress = 100
                    )
                }

                val resultResponse = tingwuApi.getTranscriptionResult(taskId)
                if (resultResponse.isSuccessful) {
                    val result = resultResponse.body()
                    val formatted = result?.transcription?.let {
                        TingwuApiHelper.formatTranscriptionForDisplay(
                            data = it,
                            includeSpeaker = true,
                            includeTimestamps = true
                        )
                    } ?: "未返回转写内容。"

                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuResultText = formatted,
                            tingwuStatusMessage = "转写成功"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isTingwuProcessing = false,
                            tingwuError = "获取转写结果失败: ${resultResponse.code()} ${resultResponse.message()}",
                            tingwuStatusMessage = "结果获取失败"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isTingwuProcessing = false,
                        tingwuError = e.message ?: "转写过程出现异常。",
                        tingwuStatusMessage = "任务异常终止"
                    )
                }
            }
        }
    }

    fun clearNotifications() {
        _uiState.update {
            it.copy(
                chatError = null,
                tingwuError = null
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
                isTingwuProcessing = false
            )
        }
    }
}

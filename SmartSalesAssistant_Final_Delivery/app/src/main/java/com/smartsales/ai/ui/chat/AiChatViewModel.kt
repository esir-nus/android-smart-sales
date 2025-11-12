package com.smartsales.ai.ui.chat

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.ai.chat.AiAttachment
import com.smartsales.ai.chat.AiChatEvent
import com.smartsales.ai.chat.AiChatRequest
import com.smartsales.ai.chat.AiChatService
import com.smartsales.ai.chat.AiExportTarget
import com.smartsales.ai.chat.AiMessage
import com.smartsales.ai.chat.AiRequestMetadata
import com.smartsales.ai.chat.AiTaskMode
import com.smartsales.ai.history.AiSession
import com.smartsales.ai.history.AiSessionRepository
import com.smartsales.ai.history.AiSessionSummary
import com.smartsales.ai.media.MediaPreprocessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@Immutable
data class AiChatUiState(
    val sessionId: String? = null,
    val title: String = "新对话",
    val prompt: String = "",
    val selectedMode: AiTaskMode = AiTaskMode.AnalyzeCustomer,
    val attachments: List<AiAttachment> = emptyList(),
    val messages: List<AiMessage> = emptyList(),
    val liveResponseMarkdown: String? = null,
    val isStreaming: Boolean = false,
    val isUploading: Boolean = false,
    val errorMessage: String? = null,
    val pendingStructuredSessionId: String? = null,
)

@HiltViewModel
class AiChatViewModel
    @Inject
    constructor(
        private val sessionRepository: AiSessionRepository,
        private val chatService: AiChatService,
        private val mediaPreprocessor: MediaPreprocessor,
    ) : ViewModel() {
        private val _state = MutableStateFlow(AiChatUiState())
        val state: StateFlow<AiChatUiState> = _state.asStateFlow()

        private var streamingJob: Job? = null

        fun onPromptChange(value: String) {
            _state.update { it.copy(prompt = value) }
        }

        fun selectMode(mode: AiTaskMode) {
            _state.update { current ->
                current.copy(
                    selectedMode = mode,
                    pendingStructuredSessionId = null,
                )
            }
        }

        fun removeAttachment(path: String) {
            _state.update { current ->
                current.copy(attachments = current.attachments.filterNot { it.localPath == path })
            }
        }

        fun importAttachment(rawPath: String) {
            viewModelScope.launch {
                _state.update { it.copy(isUploading = true, errorMessage = null) }
                runCatching { mediaPreprocessor.prepare(rawPath) }
                    .onSuccess { attachment ->
                        _state.update { state ->
                            state.copy(
                                attachments = state.attachments + attachment,
                                isUploading = false,
                            )
                        }
                    }
                    .onFailure { throwable ->
                        _state.update { it.copy(isUploading = false, errorMessage = throwable.message) }
                    }
            }
        }

        fun startNewChat() {
            streamingJob?.cancel()
            _state.value = AiChatUiState()
        }

        fun loadSession(sessionId: String) {
            if (_state.value.sessionId == sessionId && _state.value.messages.isNotEmpty()) return
            streamingJob?.cancel()
            viewModelScope.launch {
                runCatching { sessionRepository.readSession(sessionId) }
                    .onSuccess { session ->
                        if (session != null) {
                            val lastMode =
                                session.messages.filterIsInstance<AiMessage.User>().lastOrNull()?.taskMode
                                    ?: AiTaskMode.AnalyzeCustomer
                            _state.value =
                                AiChatUiState(
                                    sessionId = session.summary.id,
                                    title = session.summary.title,
                                    messages = session.messages,
                                    selectedMode = lastMode,
                                )
                        } else {
                            _state.update { it.copy(errorMessage = "会话不存在") }
                        }
                    }
                    .onFailure { throwable ->
                        _state.update { it.copy(errorMessage = throwable.message) }
                    }
            }
        }

        fun sendMessage() {
            val prompt = _state.value.prompt.trim()
            if (prompt.isEmpty()) return

            val sessionId = _state.value.sessionId ?: createNewSessionId()
            val taskMode = _state.value.selectedMode
            val attachments = _state.value.attachments
            val exportTargets = exportTargetsForMode(taskMode)

            val userMessage =
                AiMessage.User(
                    id = UUID.randomUUID().toString(),
                    createdAtMillis = System.currentTimeMillis(),
                    text = prompt,
                    attachments = attachments,
                    taskMode = taskMode,
                )

            val nextMessages = _state.value.messages + userMessage
            _state.update {
                it.copy(
                    sessionId = sessionId,
                    prompt = "",
                    messages = nextMessages,
                    isStreaming = true,
                    liveResponseMarkdown = null,
                    errorMessage = null,
                    pendingStructuredSessionId = null,
                )
            }

            persistSession(sessionId, nextMessages, _state.value.title)

            streamingJob?.cancel()
            streamingJob =
                viewModelScope.launch {
                    val request =
                        AiChatRequest(
                            sessionId = sessionId,
                            taskMode = taskMode,
                            prompt = prompt,
                            attachments = attachments,
                            metadata =
                                AiRequestMetadata(
                                    structuredOutputPreferred = taskMode.requiresStructuredScreen,
                                    exportTargets = exportTargets,
                                ),
                        )

                    try {
                        chatService.streamResponse(request).collect { event ->
                            when (event) {
                                is AiChatEvent.Chunk -> _state.update { it.copy(liveResponseMarkdown = event.markdown) }
                                is AiChatEvent.Completed ->
                                    handleAssistantCompletion(
                                        sessionId = sessionId,
                                        markdown = _state.value.liveResponseMarkdown.orEmpty(),
                                        exportTargets = exportTargets,
                                        mode = taskMode,
                                    )

                                is AiChatEvent.Error ->
                                    _state.update {
                                        it.copy(
                                            isStreaming = false,
                                            errorMessage = event.throwable.message,
                                        )
                                    }
                            }
                        }
                    } catch (throwable: Throwable) {
                        _state.update {
                            it.copy(
                                isStreaming = false,
                                errorMessage = throwable.message,
                            )
                        }
                    }
                }
        }

        fun onStructuredNavigationHandled() {
            _state.update { it.copy(pendingStructuredSessionId = null) }
        }

        private fun handleAssistantCompletion(
            sessionId: String,
            markdown: String,
            exportTargets: Set<AiExportTarget>,
            mode: AiTaskMode,
        ) {
            val assistantMessage =
                AiMessage.Assistant(
                    id = UUID.randomUUID().toString(),
                    createdAtMillis = System.currentTimeMillis(),
                    markdown = markdown,
                    exportTargets = exportTargets,
                )
            val updatedMessages = _state.value.messages + assistantMessage

            val shouldNavigateStructured = mode.requiresStructuredScreen

            _state.update {
                it.copy(
                    messages = updatedMessages,
                    liveResponseMarkdown = null,
                    isStreaming = false,
                    attachments = emptyList(),
                    pendingStructuredSessionId = if (shouldNavigateStructured) sessionId else null,
                )
            }
            persistSession(sessionId, updatedMessages, _state.value.title)
        }

        private fun createNewSessionId(): String {
            val sessionId = UUID.randomUUID().toString()
            _state.update { it.copy(sessionId = sessionId) }
            return sessionId
        }

        private fun exportTargetsForMode(mode: AiTaskMode): Set<AiExportTarget> {
            return when (mode) {
                AiTaskMode.AnalyzeCustomer -> emptySet()
                AiTaskMode.GeneratePdf -> setOf(AiExportTarget.PDF)
                AiTaskMode.GenerateCsv -> setOf(AiExportTarget.CSV)
                is AiTaskMode.Custom -> emptySet()
            }
        }

        private fun persistSession(
            sessionId: String,
            messages: List<AiMessage>,
            title: String,
        ) {
            viewModelScope.launch {
                val summary =
                    AiSessionSummary(
                        id = sessionId,
                        title = deriveTitle(title, messages),
                        timestampMillis = System.currentTimeMillis(),
                        subtitle = summarySubtitle(messages),
                    )
                val session = AiSession(summary = summary, messages = messages)
                runCatching { sessionRepository.upsertSession(session) }
                if (summary.title != _state.value.title) {
                    _state.update { it.copy(title = summary.title) }
                }
            }
        }

        private fun deriveTitle(
            defaultTitle: String,
            messages: List<AiMessage>,
        ): String {
            val firstUserMessage = messages.filterIsInstance<AiMessage.User>().firstOrNull()
            return firstUserMessage?.text?.takeIf { it.isNotBlank() }?.take(18) ?: defaultTitle
        }

        private fun summarySubtitle(messages: List<AiMessage>): String {
            val latestAssistant = messages.filterIsInstance<AiMessage.Assistant>().lastOrNull()
            return latestAssistant?.markdown?.lineSequence()?.firstOrNull()?.take(24) ?: "草稿"
        }
    }

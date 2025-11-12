package com.smartsales.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.data.local.entity.MessageEntity
import com.smartsales.data.local.repository.ConversationRepository
import com.smartsales.data.network.dashscope.DashscopeChatClient
import com.smartsales.data.network.dashscope.DashscopeChatPayload
import com.smartsales.data.network.dashscope.DashscopeStreamEvent
import com.smartsales.data.network.model.ChatInput
import com.smartsales.data.network.model.ChatMessage
import com.smartsales.data.network.model.ChatParameters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Extension function to convert MessageEntity to ChatMessage
 */
fun MessageEntity.toChatMessage(): ChatMessage {
    return when (role) {
        MessageEntity.ROLE_USER -> ChatMessage.user(content)
        MessageEntity.ROLE_ASSISTANT -> ChatMessage.assistant(content)
        MessageEntity.ROLE_SYSTEM -> ChatMessage.system(content)
        else -> ChatMessage.user(content)
    }
}

/**
 * Chat UI State
 */
data class ChatUiState(
    val conversationId: Long? = null,
    val conversationTitle: String = "新对话",
    val messages: List<MessageEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isAnalyzed: Boolean = false,
    val error: String? = null,
)

/**
 * Chat ViewModel
 */
@HiltViewModel
class ChatViewModel
    @Inject
    constructor(
        private val conversationRepository: ConversationRepository,
        private val dashscopeChatClient: DashscopeChatClient,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ChatUiState())
        val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

        private val _inputText = MutableStateFlow("")
        val inputText: StateFlow<String> = _inputText.asStateFlow()

        private var currentConversation: ConversationEntity? = null
        private var messagesJob: Job? = null

        /**
         * Load existing conversation
         */
        fun loadConversation(conversationId: Long) {
            messagesJob?.cancel()
            messagesJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isLoading = true, error = null) }

                    try {
                        // Get conversation
                        val conversation = conversationRepository.getConversationById(conversationId)

                        if (conversation != null) {
                            currentConversation = conversation

                            // Load messages
                            conversationRepository.getMessages(conversationId)
                                .collect { messages ->
                                    _uiState.update {
                                        it.copy(
                                            conversationId = conversationId,
                                            conversationTitle = conversation.title,
                                            messages = messages,
                                            isAnalyzed = conversation.isAnalyzed,
                                            isLoading = false,
                                        )
                                    }
                                }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "对话不存在",
                                )
                            }
                        }
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = e.message ?: "加载失败",
                            )
                        }
                    }
                }
        }

        /**
         * Create new conversation
         */
        fun createNewConversation() {
            viewModelScope.launch {
                try {
                    val result = conversationRepository.createConversation("新对话")

                    result.onSuccess { conversationId ->
                        _uiState.update {
                            it.copy(
                                conversationId = conversationId,
                                conversationTitle = "新对话",
                            )
                        }

                        // Load the new conversation
                        loadConversation(conversationId)
                    }

                    result.onFailure { exception ->
                        _uiState.update {
                            it.copy(error = exception.message ?: "创建失败")
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "创建失败")
                    }
                }
            }
        }

        /**
         * Update input text
         */
        fun updateInputText(text: String) {
            _inputText.value = text
        }

        /**
         * Send message
         */
        fun sendMessage() {
            val text = _inputText.value.trim()
            if (text.isBlank()) return

            val conversationId = _uiState.value.conversationId ?: return

            viewModelScope.launch {
                _uiState.update { it.copy(isSending = true, error = null) }

                try {
                    // Add user message
                    conversationRepository.addMessage(
                        conversationId = conversationId,
                        role = "user",
                        content = text,
                    )

                    // Clear input
                    _inputText.value = ""

                    // Get AI response
                    getAiResponse(conversationId, _uiState.value.messages.map { it.toChatMessage() })
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            error = e.message ?: "发送失败",
                        )
                    }
                }
            }
        }

        /**
         * Analyze customer information
         */
        fun analyzeCustomer() {
            viewModelScope.launch {
                val conversationId = _uiState.value.conversationId ?: return@launch

                try {
                    // Mark as analyzed
                    conversationRepository.markAsAnalyzed(conversationId)

                    currentConversation = currentConversation?.markAnalyzed()
                    _uiState.update { it.copy(isAnalyzed = true) }

                    // TODO: Implement actual customer analysis with AI
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = "分析失败: ${e.message}")
                    }
                }
            }
        }

        /**
         * Clear all messages in conversation
         */
        fun clearMessages() {
            viewModelScope.launch {
                val conversationId = _uiState.value.conversationId ?: return@launch

                try {
                    conversationRepository.deleteMessagesByConversation(conversationId)
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = "清空失败: ${e.message}")
                    }
                }
            }
        }

        private suspend fun getAiResponse(
            conversationId: Long,
            history: List<ChatMessage>,
        ) {
            val payload =
                DashscopeChatPayload(
                    model = "qwen-turbo",
                    input = ChatInput(history),
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
                        isSending = false,
                        error = failure?.message ?: "AI响应失败",
                    )
                }
                return
            }

            val assistantMessage =
                if (replyBuilder.isNotEmpty()) {
                    replyBuilder.toString()
                } else {
                    "（未返回内容）"
                }

            try {
                conversationRepository.addMessage(
                    conversationId = conversationId,
                    role = "assistant",
                    content = assistantMessage,
                )

                if (history.size <= 2) {
                    updateConversationTitle(conversationId, assistantMessage)
                }

                _uiState.update { it.copy(isSending = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = e.message ?: "AI响应失败",
                    )
                }
            }
        }

        private suspend fun updateConversationTitle(
            conversationId: Long,
            firstMessage: String,
        ) {
            val title = extractTitleFromMessage(firstMessage)
            conversationRepository.updateTitle(conversationId, title)
            _uiState.update { it.copy(conversationTitle = title) }
        }

        private fun extractTitleFromMessage(message: String): String {
            return message.take(20).replace(Regex("[^\\w\\s]"), "").trim()
        }
    }

package com.smartsales.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.data.local.entity.MessageEntity
import com.smartsales.data.local.repository.ConversationRepository
import com.smartsales.data.network.api.DashscopeApi
import com.smartsales.data.network.model.ChatMessage
import com.smartsales.data.network.model.createSimpleChatRequest
import com.smartsales.data.network.model.getAssistantMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val error: String? = null
)

/**
 * Chat ViewModel
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val dashscopeApi: DashscopeApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()
    
    private var currentConversation: ConversationEntity? = null
    
    /**
     * Load existing conversation
     */
    fun loadConversation(conversationId: Long) {
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
                                    isLoading = false
                                )
                            }
                        }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "对话不存在"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败"
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
                            conversationTitle = "新对话"
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
                    content = text
                )
                
                // Clear input
                _inputText.value = ""
                
                // Get AI response
                getAiResponse(conversationId)
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = e.message ?: "发送失败"
                    )
                }
            }
        }
    }
    
    /**
     * Get AI response from Dashscope
     */
    private suspend fun getAiResponse(conversationId: Long) {
        try {
            // Build message history
            val messages = _uiState.value.messages.map { message ->
                ChatMessage(
                    role = message.role,
                    content = message.content
                )
            }
            
            // Call Dashscope API
            val request = createSimpleChatRequest(messages)
            val response = dashscopeApi.chatCompletion(request)
            
            if (response.isSuccessful) {
                val chatResponse = response.body()
                val assistantMessage = chatResponse?.getAssistantMessage()
                
                if (assistantMessage != null) {
                    // Add assistant message
                    conversationRepository.addMessage(
                        conversationId = conversationId,
                        role = "assistant",
                        content = assistantMessage
                    )
                    
                    // Update title if this is the first exchange
                    if (_uiState.value.messages.size <= 2) {
                        updateConversationTitle(conversationId, assistantMessage)
                    }
                }
                
                _uiState.update { it.copy(isSending = false) }
                
            } else {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        error = "AI响应失败: ${response.code()}"
                    )
                }
            }
            
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isSending = false,
                    error = e.message ?: "AI响应失败"
                )
            }
        }
    }
    
    /**
     * Update conversation title based on first message
     */
    private suspend fun updateConversationTitle(conversationId: Long, firstMessage: String) {
        try {
            // Use first 30 characters as title
            val title = firstMessage.take(30) + if (firstMessage.length > 30) "..." else ""
            conversationRepository.updateTitle(conversationId, title)
            
            _uiState.update { it.copy(conversationTitle = title) }
        } catch (e: Exception) {
            // Ignore title update errors
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
}
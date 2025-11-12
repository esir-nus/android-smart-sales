package com.smartsales.ui.screens.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.data.local.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Conversation List UI State
 */
sealed class ConversationListUiState {
    object Loading : ConversationListUiState()

    data class Success(val conversations: List<ConversationEntity>) : ConversationListUiState()

    data class Error(val message: String) : ConversationListUiState()
}

/**
 * Conversation List ViewModel
 */
@HiltViewModel
class ConversationListViewModel
    @Inject
    constructor(
        private val conversationRepository: ConversationRepository,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow<ConversationListUiState>(
                ConversationListUiState.Loading,
            )
        val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()
        private var conversationsJob: Job? = null

        init {
            loadConversations()
        }

        private fun loadConversations() {
            conversationsJob?.cancel()
            conversationsJob =
                viewModelScope.launch {
                    conversationRepository.getAllConversations()
                        .catch { exception ->
                            _uiState.value =
                                ConversationListUiState.Error(
                                    exception.message ?: "加载失败",
                                )
                        }
                        .collect { conversations ->
                            _uiState.value = ConversationListUiState.Success(conversations)
                        }
                }
        }

        fun refreshConversations() {
            loadConversations()
        }

        fun deleteConversation(id: Long) {
            viewModelScope.launch {
                conversationRepository.deleteConversation(id)
            }
        }
    }

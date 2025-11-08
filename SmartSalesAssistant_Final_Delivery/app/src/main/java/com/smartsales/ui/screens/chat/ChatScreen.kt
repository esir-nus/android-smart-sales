package com.smartsales.ui.screens.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.data.local.entity.MessageEntity
import com.smartsales.ui.components.LoadingIndicator
import com.smartsales.ui.components.MessageBubble
import com.smartsales.ui.components.TypingIndicator

/**
 * Chat Screen
 * 
 * Main conversation screen with AI chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: Long,
    onNavigateBack: () -> Unit,
    onExportClick: (Long) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    
    val showMenu = remember { mutableStateOf(false) }
    
    LaunchedEffect(conversationId) {
        if (conversationId > 0) {
            viewModel.loadConversation(conversationId)
        } else {
            viewModel.createNewConversation()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.conversationTitle,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (uiState.isAnalyzed) {
                            Text(
                                text = "已分析",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // More options menu
                    IconButton(onClick = { showMenu.value = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu.value,
                        onDismissRequest = { showMenu.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("分析客户") },
                            onClick = {
                                showMenu.value = false
                                viewModel.analyzeCustomer()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Analytics, "分析")
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("导出对话") },
                            onClick = {
                                showMenu.value = false
                                uiState.conversationId?.let { onExportClick(it) }
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Download, "导出")
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("清空对话") },
                            onClick = {
                                showMenu.value = false
                                viewModel.clearMessages()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, "清空")
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = inputText,
                onTextChange = { viewModel.updateInputText(it) },
                onSendClick = { viewModel.sendMessage() },
                enabled = !uiState.isSending,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        message = "加载中..."
                    )
                }
                
                uiState.error != null -> {
                    ErrorMessage(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadConversation(conversationId) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                else -> {
                    MessageList(
                        messages = uiState.messages,
                        isTyping = uiState.isSending,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<MessageEntity>,
    isTyping: Boolean,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        val lastIndex = messages.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }
    
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            MessageBubble(message = message)
        }
        
        if (isTyping) {
            item {
                TypingIndicator()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Text input
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息...") },
                enabled = enabled,
                maxLines = 4
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Send button
            FilledIconButton(
                onClick = onSendClick,
                enabled = enabled && text.isNotBlank(),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "发送"
                )
            }
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = onRetry) {
            Text("重试")
        }
    }
}

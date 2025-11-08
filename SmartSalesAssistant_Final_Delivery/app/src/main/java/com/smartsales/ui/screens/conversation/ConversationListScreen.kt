package com.smartsales.ui.screens.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsales.data.local.entity.ConversationEntity
import com.smartsales.ui.components.EmptyState
import com.smartsales.ui.components.LoadingIndicator
import com.smartsales.ui.components.NoConversationsEmptyState

/**
 * Conversation List Screen
 * 
 * Shows all conversations grouped by time
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    onConversationClick: (Long) -> Unit,
    onNewConversation: () -> Unit,
    viewModel: ConversationListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("对话列表") },
                actions = {
                    IconButton(onClick = { viewModel.refreshConversations() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新列表"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewConversation,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "新建对话"
                )
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ConversationListUiState.Loading -> {
                LoadingIndicator(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            
            is ConversationListUiState.Success -> {
                if (state.conversations.isEmpty()) {
                    NoConversationsEmptyState(
                        onCreateConversation = onNewConversation,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                } else {
                    ConversationList(
                        conversations = state.conversations,
                        onConversationClick = onConversationClick,
                        onDeleteConversation = { conversation ->
                            conversationToDelete = conversation
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
            
            is ConversationListUiState.Error -> {
                EmptyState(
                    message = state.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }

    val pendingDeletion = conversationToDelete
    if (pendingDeletion != null) {
        AlertDialog(
            onDismissRequest = { conversationToDelete = null },
            title = { Text("删除对话") },
            text = {
                Text("确定要删除对话 \"${pendingDeletion.title}\" 吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(pendingDeletion.id)
                        conversationToDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { conversationToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ConversationList(
    conversations: List<ConversationEntity>,
    onConversationClick: (Long) -> Unit,
    onDeleteConversation: (ConversationEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group conversations by date
    val groupedConversations = conversations.groupBy { conversation ->
        when {
            conversation.isUpdatedToday -> "今天"
            conversation.ageInDays == 1 -> "昨天"
            conversation.ageInDays < 7 -> "本周"
            conversation.ageInDays < 30 -> "本月"
            else -> "更早"
        }
    }
    
    val groups = listOf("今天", "昨天", "本周", "本月", "更早")
    
    LazyColumn(modifier = modifier) {
        groups.forEach { group ->
            val groupConversations = groupedConversations[group]
            if (!groupConversations.isNullOrEmpty()) {
                // Group header
                item(key = "header_$group") {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                // Conversations in group
                items(
                    items = groupConversations,
                    key = { it.id }
                ) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                        onDelete = { onDeleteConversation(conversation) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = if (conversation.isAnalyzed) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (conversation.isAnalyzed) "✓" else "·",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (conversation.isAnalyzed) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Conversation info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (conversation.hasSummary) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = conversation.summary!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = conversation.getRelativeTimeString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "删除对话",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    Divider()
}

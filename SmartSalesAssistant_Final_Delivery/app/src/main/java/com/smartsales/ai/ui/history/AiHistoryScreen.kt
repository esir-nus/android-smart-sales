package com.smartsales.ai.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.ai.history.AiSessionSummary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AiHistoryRoute(
    onBack: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: AiHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sessionToDelete = remember { mutableStateOf<AiSessionSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> CircularProgressIndicator(modifier = Modifier.padding(innerPadding).padding(24.dp))
            state.errorMessage != null ->
                Text(
                    text = state.errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(innerPadding).padding(24.dp),
                )
            else ->
                HistoryList(
                    groups = state.groups,
                    onSessionClick = onSessionClick,
                    onSessionLongPress = { summary -> sessionToDelete.value = summary },
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                )
        }
    }

    sessionToDelete.value?.let { summary ->
        AlertDialog(
            onDismissRequest = { sessionToDelete.value = null },
            title = { Text("删除记录") },
            text = { Text("确定删除 \"${summary.title}\" 吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(summary.id)
                        sessionToDelete.value = null
                    },
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete.value = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryList(
    groups: List<HistoryGroup>,
    onSessionClick: (String) -> Unit,
    onSessionLongPress: (AiSessionSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        groups.forEach { group ->
            item {
                Text(
                    text = group.label,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(MaterialTheme.colorScheme.background),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            items(group.items) { summary ->
                HistoryCard(summary = summary, onSessionClick = onSessionClick, onSessionLongPress = onSessionLongPress)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    summary: AiSessionSummary,
    onSessionClick: (String) -> Unit,
    onSessionLongPress: (AiSessionSummary) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { onSessionClick(summary.id) },
                    onLongClick = { onSessionLongPress(summary) },
                ),
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = summary.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = summary.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        }
    }
}

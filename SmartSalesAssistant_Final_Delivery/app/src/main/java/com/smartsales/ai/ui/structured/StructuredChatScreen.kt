package com.smartsales.ai.ui.structured

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsales.ai.chat.AiExportTarget

@Composable
fun StructuredChatRoute(
    sessionId: String,
    onBack: () -> Unit,
    viewModel: StructuredChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.load(sessionId)
    }

    StructuredChatScreen(
        state = state,
        onBack = onBack,
        onExport = viewModel::export,
        onExportNoticeConsumed = viewModel::clearExportState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StructuredChatScreen(
    state: StructuredChatUiState,
    onBack: () -> Unit,
    onExport: (AiExportTarget) -> Unit,
    onExportNoticeConsumed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.exportState) {
        when (val exportState = state.exportState) {
            is ExportUiState.Completed -> {
                snackbarHostState.showSnackbar("导出成功: ${exportState.filePath}")
                onExportNoticeConsumed()
            }

            is ExportUiState.Failed -> {
                snackbarHostState.showSnackbar(exportState.message)
                onExportNoticeConsumed()
            }

            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    ExportButtons(onExport = onExport, exportState = state.exportState)
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingState(modifier = Modifier.padding(innerPadding))
            state.errorMessage != null ->
                ErrorState(
                    message = state.errorMessage,
                    modifier = Modifier.padding(innerPadding),
                )
            else ->
                SectionList(
                    sections = state.sections,
                    modifier =
                        Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Text(text = "加载结构化内容…", modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun SectionList(
    sections: List<StructuredSectionUi>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(sections) { section ->
            Surface(shape = MaterialTheme.shapes.large, tonalElevation = 1.dp) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = section.heading, style = MaterialTheme.typography.titleMedium)
                    if (section.progress != null) {
                        LinearProgressIndicator(progress = section.progress)
                    }
                    Text(text = section.description, style = MaterialTheme.typography.bodyMedium)
                    if (section.keyFindings.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        section.keyFindings.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = key, style = MaterialTheme.typography.labelMedium)
                                Text(text = value, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportButtons(
    onExport: (AiExportTarget) -> Unit,
    exportState: ExportUiState,
) {
    val exportingTarget = (exportState as? ExportUiState.InFlight)?.target
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(AiExportTarget.PDF, AiExportTarget.CSV, AiExportTarget.MIND_MAP).forEach { target ->
            OutlinedButton(
                enabled = exportingTarget == null,
                onClick = { onExport(target) },
            ) {
                Text(text = target.name)
            }
        }
    }
}

package com.smartsales.ai.ui.structured

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.ai.chat.AiExportTarget
import com.smartsales.ai.export.ExportDestination
import com.smartsales.ai.export.ExportManager
import com.smartsales.ai.export.StructuredMarkdown
import com.smartsales.ai.history.AiSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@Immutable
data class StructuredChatUiState(
    val sessionId: String? = null,
    val title: String = "结构化反馈",
    val markdown: String? = null,
    val sections: List<StructuredSectionUi> = emptyList(),
    val exportState: ExportUiState = ExportUiState.Idle,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class StructuredChatViewModel
    @Inject
    constructor(
        private val sessionRepository: AiSessionRepository,
        private val exportManager: ExportManager,
    ) : ViewModel() {
        private val _state = MutableStateFlow(StructuredChatUiState())
        val state: StateFlow<StructuredChatUiState> = _state.asStateFlow()

        fun load(sessionId: String) {
            if (_state.value.sessionId == sessionId && !_state.value.isLoading) return

            viewModelScope.launch {
                _state.update { it.copy(isLoading = true, errorMessage = null) }
                val session = sessionRepository.readSession(sessionId)
                if (session == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "未找到会话",
                        )
                    }
                    return@launch
                }

                val markdown = session.messages.filterIsInstance<com.smartsales.ai.chat.AiMessage.Assistant>().lastOrNull()?.markdown
                val sections = parseSections(markdown.orEmpty())

                _state.update {
                    it.copy(
                        sessionId = sessionId,
                        title = session.summary.title,
                        markdown = markdown,
                        sections = sections,
                        isLoading = false,
                        errorMessage = null,
                        exportState = ExportUiState.Idle,
                    )
                }
            }
        }

        fun export(target: AiExportTarget) {
            val markdown = _state.value.markdown ?: return
            viewModelScope.launch {
                _state.update { it.copy(exportState = ExportUiState.InFlight(target)) }
                val destination =
                    ExportDestination(
                        fileName = "ai_${target.name.lowercase(Locale.ROOT)}_${System.currentTimeMillis()}",
                        directoryType = ExportDestination.DirectoryType.DOCUMENTS,
                    )
                val result =
                    exportManager.export(
                        content = StructuredMarkdown(raw = markdown),
                        target = target,
                        destination = destination,
                    )
                val newState =
                    when (result) {
                        is com.smartsales.ai.export.ExportResult.Success -> ExportUiState.Completed(result.absolutePath)
                        is com.smartsales.ai.export.ExportResult.Failure ->
                            ExportUiState.Failed(
                                result.throwable.message ?: "导出失败",
                            )
                    }
                _state.update { it.copy(exportState = newState) }
            }
        }

        fun clearExportState() {
            _state.update { it.copy(exportState = ExportUiState.Idle) }
        }

        private fun parseSections(markdown: String): List<StructuredSectionUi> {
            if (markdown.isBlank()) return emptyList()
            val sections = markdown.split("## ").filter { it.isNotBlank() }
            return sections.map { block ->
                val lines = block.lines()
                val heading = lines.first().trim()
                val description = lines.drop(1).joinToString("\n").trim()
                StructuredSectionUi(
                    heading = heading,
                    description = description,
                )
            }
        }
    }

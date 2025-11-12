package com.smartsales.ai.ui.structured

import androidx.compose.runtime.Immutable
import com.smartsales.ai.chat.AiExportTarget

@Immutable
data class StructuredSectionUi(
    val heading: String,
    val description: String,
    val keyFindings: List<Pair<String, String>> = emptyList(),
    val progress: Float? = null,
)

sealed interface ExportUiState {
    data object Idle : ExportUiState

    data class InFlight(val target: AiExportTarget) : ExportUiState

    data class Completed(val filePath: String) : ExportUiState

    data class Failed(val message: String) : ExportUiState
}

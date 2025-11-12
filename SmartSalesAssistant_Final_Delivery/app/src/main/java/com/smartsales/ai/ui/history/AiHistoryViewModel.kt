package com.smartsales.ai.ui.history

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.ai.history.AiSessionRepository
import com.smartsales.ai.history.AiSessionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Immutable
data class AiHistoryUiState(
    val groups: List<HistoryGroup> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@Immutable
data class HistoryGroup(
    val label: String,
    val items: List<AiSessionSummary>,
)

@HiltViewModel
class AiHistoryViewModel
    @Inject
    constructor(
        private val sessionRepository: AiSessionRepository,
    ) : ViewModel() {
        val state: StateFlow<AiHistoryUiState> =
            sessionRepository.observeSummaries()
                .map { summaries ->
                    val groups = buildGroups(summaries)
                    AiHistoryUiState(groups = groups, isLoading = false)
                }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = AiHistoryUiState(),
                )

        fun deleteSession(sessionId: String) {
            viewModelScope.launch {
                runCatching { sessionRepository.deleteSession(sessionId) }
            }
        }

        private fun buildGroups(summaries: List<AiSessionSummary>): List<HistoryGroup> {
            val sorted = summaries.sortedByDescending { it.timestampMillis }
            val now = System.currentTimeMillis()
            val sevenDaysMillis = 7 * 24 * 60 * 60 * 1000L
            val thirtyDaysMillis = 30 * 24 * 60 * 60 * 1000L
            val formatter = SimpleDateFormat("yyyy年M月", Locale.CHINA)
            val groups = linkedMapOf<String, MutableList<AiSessionSummary>>()
            for (summary in sorted) {
                val delta = now - summary.timestampMillis
                val label =
                    when {
                        delta <= sevenDaysMillis -> "7天内"
                        delta <= thirtyDaysMillis -> "30天内"
                        else -> formatter.format(Date(summary.timestampMillis))
                    }
                groups.getOrPut(label) { mutableListOf() }.add(summary)
            }
            return groups.map { (label, items) ->
                HistoryGroup(label = label, items = items)
            }
        }
    }

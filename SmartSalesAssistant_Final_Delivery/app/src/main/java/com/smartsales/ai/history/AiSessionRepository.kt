package com.smartsales.ai.history

import com.smartsales.ai.chat.AiMessage
import kotlinx.coroutines.flow.Flow

/**
 * Persists chat sessions for the History screen (sticky headers, sorting, etc.).
 */
interface AiSessionRepository {
    fun observeSummaries(): Flow<List<AiSessionSummary>>

    suspend fun readSession(sessionId: String): AiSession?

    suspend fun upsertSession(session: AiSession)

    suspend fun deleteSession(sessionId: String)
}

data class AiSessionSummary(
    val id: String,
    val title: String,
    val timestampMillis: Long,
    val subtitle: String,
)

data class AiSession(
    val summary: AiSessionSummary,
    val messages: List<AiMessage>,
)

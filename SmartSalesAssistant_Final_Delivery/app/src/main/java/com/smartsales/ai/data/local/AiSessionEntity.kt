package com.smartsales.ai.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_sessions")
data class AiSessionEntity(
    @PrimaryKey val sessionId: String,
    val title: String,
    val subtitle: String,
    val timestampMillis: Long,
    val messagesJson: String,
)

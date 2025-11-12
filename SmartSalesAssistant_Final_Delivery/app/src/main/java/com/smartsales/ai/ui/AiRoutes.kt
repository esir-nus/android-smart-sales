package com.smartsales.ai.ui

/**
 * Centralizes the navigation destinations outlined in AI_UI_specifications.md.
 */
object AiRoutes {
    private const val CHAT_BASE = "ai/chat"
    const val CHAT_SESSION_ARG = "sessionId"
    const val CHAT_ROUTE = "$CHAT_BASE/{$CHAT_SESSION_ARG}"

    const val NEW_SESSION_ID = "new"

    fun chat(sessionId: String = NEW_SESSION_ID): String = "$CHAT_BASE/$sessionId"

    private const val STRUCTURED_BASE = "ai/chat/structured"
    const val STRUCTURED_SESSION_ARG = "sessionId"
    const val STRUCTURED_ROUTE = "$STRUCTURED_BASE/{$STRUCTURED_SESSION_ARG}"

    fun structured(sessionId: String): String = "$STRUCTURED_BASE/$sessionId"

    const val HISTORY = "ai/history"
}

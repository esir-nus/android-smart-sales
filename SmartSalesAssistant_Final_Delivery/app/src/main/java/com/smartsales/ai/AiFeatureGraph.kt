package com.smartsales.ai

import com.smartsales.ai.chat.AiChatService
import com.smartsales.ai.export.ExportManager
import com.smartsales.ai.history.AiSessionRepository
import com.smartsales.ai.oss.OssRepository
import com.smartsales.ai.tingwu.TingwuCoordinator

/**
 * Aggregates the contracts the AI feature exposes to the rest of the app.
 *
 * Keeping this interface lean helps the future “mother project” integrate the AI
 * and WiFi/BLE child projects without tightly coupling implementation details.
 */
interface AiFeatureGraph {
    val chatService: AiChatService
    val sessionRepository: AiSessionRepository
    val exportManager: ExportManager
    val tingwuCoordinator: TingwuCoordinator
    val ossRepository: OssRepository
}

package com.smartsales.ai

import com.smartsales.ai.chat.AiChatService
import com.smartsales.ai.export.ExportManager
import com.smartsales.ai.history.AiSessionRepository
import com.smartsales.ai.oss.OssRepository
import com.smartsales.ai.tingwu.TingwuCoordinator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAiFeatureGraph
    @Inject
    constructor(
        override val chatService: AiChatService,
        override val sessionRepository: AiSessionRepository,
        override val exportManager: ExportManager,
        override val tingwuCoordinator: TingwuCoordinator,
        override val ossRepository: OssRepository,
    ) : AiFeatureGraph

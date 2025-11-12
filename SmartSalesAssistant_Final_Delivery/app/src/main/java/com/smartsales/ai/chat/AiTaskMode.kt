package com.smartsales.ai.chat

/**
 * Mirrors the three skill buttons defined in AI_UI_specifications.md and keeps room
 * for future custom modes when the two child projects merge.
 */
sealed class AiTaskMode(val displayName: String, val requiresStructuredScreen: Boolean) {
    data object AnalyzeCustomer : AiTaskMode(displayName = "分析客户", requiresStructuredScreen = false)

    data object GeneratePdf : AiTaskMode(displayName = "生成 PDF", requiresStructuredScreen = true)

    data object GenerateCsv : AiTaskMode(displayName = "生成 CSV", requiresStructuredScreen = true)

    data class Custom(
        val id: String,
        val shouldPersistAsTemplate: Boolean,
        val structured: Boolean,
        val label: String,
    ) : AiTaskMode(displayName = label, requiresStructuredScreen = structured)
}

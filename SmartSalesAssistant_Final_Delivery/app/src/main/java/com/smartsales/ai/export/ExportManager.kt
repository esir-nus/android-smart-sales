package com.smartsales.ai.export

import com.smartsales.ai.chat.AiExportTarget

/**
 * Converts structured markdown responses into shareable documents.
 */
interface ExportManager {
    suspend fun export(
        content: StructuredMarkdown,
        target: AiExportTarget,
        destination: ExportDestination,
    ): ExportResult
}

/**
 * Wrapper for the markdown that the DashScope/Tingwu pipeline produces.
 */
data class StructuredMarkdown(
    val raw: String,
    val blocks: List<MarkdownBlock> = emptyList(),
)

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock

    data class Paragraph(val text: String) : MarkdownBlock

    data class BulletList(val items: List<String>) : MarkdownBlock

    data class KeyValueTable(val rows: List<Pair<String, String>>) : MarkdownBlock
}

data class ExportDestination(
    val fileName: String,
    val directoryType: DirectoryType,
) {
    enum class DirectoryType {
        CACHE,
        DOCUMENTS,
    }
}

sealed interface ExportResult {
    data class Success(val absolutePath: String) : ExportResult

    data class Failure(val throwable: Throwable) : ExportResult
}

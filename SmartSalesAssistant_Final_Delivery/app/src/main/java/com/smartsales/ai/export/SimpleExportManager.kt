package com.smartsales.ai.export

import android.content.Context
import android.os.Environment
import com.smartsales.ai.chat.AiExportTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Lightweight export helper that writes markdown/CSV placeholders so UX flows can be
 * validated before we wire the formal PDF/CSV pipeline.
 */
class SimpleExportManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ExportManager {
        override suspend fun export(
            content: StructuredMarkdown,
            target: AiExportTarget,
            destination: ExportDestination,
        ): ExportResult {
            return runCatching {
                withContext(Dispatchers.IO) {
                    val directory = resolveDirectory(destination.directoryType)
                    if (!directory.exists()) {
                        directory.mkdirs()
                    }
                    val file = File(directory, fileName(destination.fileName, target))
                    val payload =
                        when (target) {
                            AiExportTarget.PDF -> content.raw
                            AiExportTarget.CSV -> csvPayload(content)
                            AiExportTarget.MIND_MAP -> mindMapPayload(content)
                        }
                    file.writeText(payload, Charsets.UTF_8)
                    ExportResult.Success(file.absolutePath)
                }
            }.getOrElse { throwable ->
                ExportResult.Failure(throwable)
            }
        }

        private fun resolveDirectory(type: ExportDestination.DirectoryType): File {
            return when (type) {
                ExportDestination.DirectoryType.CACHE -> context.cacheDir
                ExportDestination.DirectoryType.DOCUMENTS ->
                    context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        ?: File(context.filesDir, "documents").also { it.mkdirs() }
            }
        }

        private fun fileName(
            base: String,
            target: AiExportTarget,
        ): String {
            val suffix =
                when (target) {
                    AiExportTarget.PDF -> ".pdf"
                    AiExportTarget.CSV -> ".csv"
                    AiExportTarget.MIND_MAP -> ".mm"
                }
            return if (base.lowercase(Locale.ROOT).endsWith(suffix)) base else base + suffix
        }

        private fun csvPayload(content: StructuredMarkdown): String {
            val rows =
                content.blocks
                    .filterIsInstance<MarkdownBlock.KeyValueTable>()
                    .firstOrNull()
                    ?.rows
                    ?: return "column,value\n\"content\",\"${sanitize(content.raw)}\""
            val builder = StringBuilder("column,value\n")
            rows.forEach { (key, value) ->
                builder.append('"')
                    .append(sanitize(key))
                    .append("\",\"")
                    .append(sanitize(value))
                    .append("\"\n")
            }
            return builder.toString()
        }

        private fun mindMapPayload(content: StructuredMarkdown): String {
            val items =
                content.blocks
                    .filterIsInstance<MarkdownBlock.BulletList>()
                    .firstOrNull()
                    ?.items
                    ?: listOf(content.raw)
            return buildString {
                append("{\"root\":\"AI Summary\",\"nodes\":[")
                append(items.joinToString(",") { "\"${sanitize(it)}\"" })
                append("]}")
            }
        }

        private fun sanitize(value: String): String {
            return value.replace("\"", "\\\"").replace("\n", " ")
        }
    }

package com.smartsales.ai.media

import android.content.Context
import android.net.Uri
import com.smartsales.ai.chat.AiAttachment
import com.smartsales.ai.chat.AiAttachmentType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.util.Locale
import javax.inject.Inject

/**
 * Basic attachment validator that enforces the mp3/image whitelist and surfaces enough
 * metadata for DashScope uploads. Real preprocessing (compression, OSS staging) will
 * replace this once the Ali SDK workflow is wired.
 */
class DefaultMediaPreprocessor
    @Inject
    constructor(
        @ApplicationContext private val appContext: Context,
    ) : MediaPreprocessor {
        override suspend fun prepare(rawAttachmentPath: String): AiAttachment {
            return withContext(Dispatchers.IO) {
                val uri = Uri.parse(rawAttachmentPath)
                val file = resolveFile(uri)
                val type = detectType(file)
                AiAttachment(
                    localPath = file.absolutePath,
                    mimeType = mimeTypeOf(type),
                    type = type,
                    sizeBytes = file.length(),
                )
            }
        }

        private fun resolveFile(uri: Uri): File {
            return when (uri.scheme) {
                null, "file" -> File(uri.path ?: throw FileNotFoundException("无效文件路径"))
                "content" -> {
                    appContext.contentResolver.openInputStream(uri)?.use { input ->
                        val cacheFile = File.createTempFile("ai-upload-", ".tmp", appContext.cacheDir)
                        cacheFile.outputStream().use { output -> input.copyTo(output) }
                        cacheFile
                    } ?: throw FileNotFoundException("无法读取文件: $uri")
                }
                else -> throw IllegalArgumentException("不支持的 URI: $uri")
            }
        }

        private fun detectType(file: File): AiAttachmentType {
            val ext = file.extension.lowercase(Locale.ROOT)
            return when (ext) {
                "png", "jpg", "jpeg", "webp" -> AiAttachmentType.IMAGE
                "mp3" -> AiAttachmentType.AUDIO
                else -> throw IllegalArgumentException("仅支持图片或 mp3，当前为 .$ext")
            }
        }

        private fun mimeTypeOf(type: AiAttachmentType): String {
            return when (type) {
                AiAttachmentType.IMAGE -> "image/*"
                AiAttachmentType.AUDIO -> "audio/mpeg"
            }
        }
    }

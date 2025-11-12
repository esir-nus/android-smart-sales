package com.smartsales.aitest.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

/**
 * Audio file picker utility for selecting local audio files
 */
class AudioFilePicker {
    companion object {
        // Supported audio MIME types
        private val SUPPORTED_AUDIO_TYPES =
            arrayOf(
                "audio/*",
                "audio/mpeg", // MP3
                "audio/wav", // WAV
                "audio/mp4", // M4A
                "audio/flac", // FLAC
                "audio/aac", // AAC
                "audio/ogg", // OGG
            )

        /**
         * Create file picker intent for audio files
         */
        fun createAudioPickerIntent(): Intent {
            return Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_MIME_TYPES, SUPPORTED_AUDIO_TYPES)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
            }
        }

        /**
         * Get file from URI
         */
        fun getFileFromUri(
            context: Context,
            uri: Uri,
        ): File? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = getFileName(context, uri) ?: "audio_${System.currentTimeMillis()}.mp3"
                val tempFile = File(context.getExternalFilesDir(null), fileName)

                inputStream?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                tempFile
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        /**
         * Get file name from URI
         */
        private fun getFileName(
            context: Context,
            uri: Uri,
        ): String? {
            var fileName: String? = null

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }

            return fileName
        }

        /**
         * Validate audio file
         */
        fun validateAudioFile(file: File): Result<Unit> {
            if (!file.exists()) {
                return Result.failure(Exception("文件不存在"))
            }

            if (file.length() > 100 * 1024 * 1024) { // 100MB limit
                return Result.failure(Exception("文件大小超过100MB限制"))
            }

            val extension = file.extension.lowercase()
            val supportedExtensions = listOf("mp3", "wav", "m4a", "flac", "aac", "ogg")

            if (extension !in supportedExtensions) {
                return Result.failure(Exception("不支持的音频格式: $extension"))
            }

            return Result.success(Unit)
        }
    }
}

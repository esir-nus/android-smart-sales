package com.smartsales.ai.oss

import kotlinx.coroutines.flow.Flow

/**
 * Minimal abstraction over the Ali OSS SDK so other modules can reuse uploads.
 */
interface OssRepository {
    fun upload(request: OssUploadRequest): Flow<OssUploadEvent>
}

data class OssUploadRequest(
    val localPath: String,
    val remoteKey: String,
    val contentType: String,
    val metadata: Map<String, String> = emptyMap(),
)

sealed interface OssUploadEvent {
    data class Progress(val bytesUploaded: Long, val totalBytes: Long) : OssUploadEvent

    data class Completed(val remoteUrl: String) : OssUploadEvent

    data class Failed(val throwable: Throwable) : OssUploadEvent
}

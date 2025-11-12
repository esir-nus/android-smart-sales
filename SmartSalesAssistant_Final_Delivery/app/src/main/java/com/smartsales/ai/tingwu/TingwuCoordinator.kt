package com.smartsales.ai.tingwu

import kotlinx.coroutines.flow.Flow

/**
 * Handles Tingwu summarization jobs and mind-map generations before uploading
 * files via Ali OSS.
 */
interface TingwuCoordinator {
    fun observeJob(jobId: String): Flow<TingwuJobState>

    suspend fun enqueue(request: TingwuRequest): String
}

data class TingwuRequest(
    val sourceFilePath: String,
    val desiredOutputs: Set<TingwuOutputType>,
    val metadata: Map<String, String> = emptyMap(),
)

enum class TingwuOutputType {
    PDF,
    CSV,
    MIND_MAP,
}

sealed interface TingwuJobState {
    data class Queued(val jobId: String) : TingwuJobState

    data class InProgress(val jobId: String, val progress: Float) : TingwuJobState

    data class Completed(val jobId: String, val artifactUris: Map<TingwuOutputType, String>) : TingwuJobState

    data class Failed(val jobId: String, val throwable: Throwable) : TingwuJobState
}

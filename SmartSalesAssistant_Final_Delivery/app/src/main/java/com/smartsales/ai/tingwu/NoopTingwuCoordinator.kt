package com.smartsales.ai.tingwu

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder Tingwu coordinator. Emits a failed state immediately so the UI can
 * surface proper messaging until the official SDK workflow is wired.
 */
@Singleton
class NoopTingwuCoordinator
    @Inject
    constructor() : TingwuCoordinator {
        private val events = MutableSharedFlow<TingwuJobState>(extraBufferCapacity = 64)

        override fun observeJob(jobId: String): Flow<TingwuJobState> {
            return events.filter { it.belongsTo(jobId) }
        }

        override suspend fun enqueue(request: TingwuRequest): String {
            val jobId = UUID.randomUUID().toString()
            events.emit(
                TingwuJobState.Failed(
                    jobId = jobId,
                    throwable = UnsupportedOperationException("Tingwu SDK integration pending"),
                ),
            )
            return jobId
        }
    }

private fun TingwuJobState.belongsTo(jobId: String): Boolean {
    return when (this) {
        is TingwuJobState.Queued -> this.jobId == jobId
        is TingwuJobState.InProgress -> this.jobId == jobId
        is TingwuJobState.Completed -> this.jobId == jobId
        is TingwuJobState.Failed -> this.jobId == jobId
    }
}

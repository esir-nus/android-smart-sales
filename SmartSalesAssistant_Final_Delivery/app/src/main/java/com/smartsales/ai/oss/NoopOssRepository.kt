package com.smartsales.ai.oss

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder Ali OSS repository. Emits a failure event so callers can surface
 * proper messaging until real uploads are available.
 */
@Singleton
class NoopOssRepository
    @Inject
    constructor() : OssRepository {
        override fun upload(request: OssUploadRequest): Flow<OssUploadEvent> =
            flow {
                emit(
                    OssUploadEvent.Failed(
                        UnsupportedOperationException("Ali OSS SDK integration pending"),
                    ),
                )
            }
    }

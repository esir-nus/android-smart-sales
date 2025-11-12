package com.smartsales.data.network.dashscope

import com.smartsales.data.network.model.ChatInput
import com.smartsales.data.network.model.ChatParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

/**
 * Provides a high-level streaming chat client backed by the official DashScope SDK.
 *
 * The client retrieves credentials (AppKey + Token) via [DashscopeCredentialProvider],
 * opens a streaming session through [DashscopeStreamingSdk], and exposes chunks as
 * a cold [Flow] so callers can integrate with coroutines.
 */
interface DashscopeChatClient {
    fun streamChat(payload: DashscopeChatPayload): Flow<DashscopeStreamEvent>
}

data class DashscopeCredentials(
    val appKey: String,
    val token: String,
)

fun interface DashscopeCredentialProvider {
    suspend fun getCredentials(): DashscopeCredentials
}

/**
 * Payload sent to the DashScope SDK. The official SDK expects structured parameters, so we reuse
 * our existing Retrofit models to avoid redefining schemas in multiple places.
 */
data class DashscopeChatPayload(
    val model: String,
    val input: ChatInput,
    val parameters: ChatParameters,
)

sealed interface DashscopeStreamEvent {
    data class Chunk(val markdown: String) : DashscopeStreamEvent

    data class Completed(val usageTokens: Int, val requestId: String?) : DashscopeStreamEvent

    data class Error(val throwable: Throwable) : DashscopeStreamEvent
}

/**
 * Abstraction over the official SDK so we can unit-test behaviour without pulling real network
 * calls or JNI bindings into the test process.
 */
interface DashscopeStreamingSdk {
    fun startChat(
        credentials: DashscopeCredentials,
        payload: DashscopeChatPayload,
        listener: DashscopeStreamingListener,
    ): DashscopeStreamingSession
}

interface DashscopeStreamingSession {
    fun cancel()
}

interface DashscopeStreamingListener {
    fun onContentChunk(markdown: String)

    fun onCompleted(
        totalTokens: Int,
        requestId: String?,
    )

    fun onError(throwable: Throwable)
}

/**
 * Default implementation that converts SDK callbacks into coroutine-friendly events.
 */
class DefaultDashscopeChatClient(
    private val credentialProvider: DashscopeCredentialProvider,
    private val sdk: DashscopeStreamingSdk,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : DashscopeChatClient {
    override fun streamChat(payload: DashscopeChatPayload): Flow<DashscopeStreamEvent> =
        callbackFlow {
            val credentials =
                try {
                    withContext(ioDispatcher) { credentialProvider.getCredentials() }
                } catch (t: Throwable) {
                    trySend(DashscopeStreamEvent.Error(t))
                    close(t)
                    return@callbackFlow
                }

            val session =
                try {
                    sdk.startChat(
                        credentials = credentials,
                        payload = payload,
                        listener =
                            object : DashscopeStreamingListener {
                                override fun onContentChunk(markdown: String) {
                                    trySend(DashscopeStreamEvent.Chunk(markdown))
                                }

                                override fun onCompleted(
                                    totalTokens: Int,
                                    requestId: String?,
                                ) {
                                    trySend(DashscopeStreamEvent.Completed(totalTokens, requestId))
                                    close()
                                }

                                override fun onError(throwable: Throwable) {
                                    trySend(DashscopeStreamEvent.Error(throwable))
                                    close(throwable)
                                }
                            },
                    )
                } catch (t: Throwable) {
                    trySend(DashscopeStreamEvent.Error(t))
                    close(t)
                    return@callbackFlow
                }

            awaitClose {
                session.cancel()
            }
        }
}

package com.smartsales.data.network.dashscope

import com.smartsales.data.network.model.ChatInput
import com.smartsales.data.network.model.ChatMessage
import com.smartsales.data.network.model.ChatParameters
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultDashscopeChatClientTest {
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private val fakeSdk = FakeDashscopeStreamingSdk()
    private val credentialProvider = RecordingCredentialProvider()
    private val client =
        DefaultDashscopeChatClient(
            credentialProvider = credentialProvider,
            sdk = fakeSdk,
            ioDispatcher = dispatcher,
        )

    @Test
    fun `emits chunks then completion`() =
        scope.runTest {
            val request =
                DashscopeChatPayload(
                    model = "qwen-turbo",
                    input =
                        ChatInput(
                            messages =
                                listOf(
                                    ChatMessage.system("system prompt"),
                                    ChatMessage.user("hello there"),
                                ),
                        ),
                    parameters = ChatParameters.streaming(),
                )

            val emitted = mutableListOf<DashscopeStreamEvent>()
            val collectJob = launch { client.streamChat(request).toList(emitted) }

            fakeSdk.emit(DashscopeStreamingCallback.Chunk("partial-1"))
            fakeSdk.emit(DashscopeStreamingCallback.Chunk("partial-2"))
            fakeSdk.emit(
                DashscopeStreamingCallback.Completed(
                    totalTokens = 128,
                    requestId = "req-123",
                ),
            )

            collectJob.join()

            assertEquals(1, credentialProvider.callCount)
            assertEquals(
                listOf(
                    DashscopeStreamEvent.Chunk("partial-1"),
                    DashscopeStreamEvent.Chunk("partial-2"),
                    DashscopeStreamEvent.Completed(usageTokens = 128, requestId = "req-123"),
                ),
                emitted,
            )
        }

    @Test
    fun `propagates sdk error`() =
        scope.runTest {
            val request =
                DashscopeChatPayload(
                    model = "qwen-plus",
                    input = ChatInput(listOf(ChatMessage.user("trigger error"))),
                    parameters = ChatParameters.default(),
                )

            val result = mutableListOf<DashscopeStreamEvent>()
            val collectJob = launch { client.streamChat(request).toList(result) }

            val failure = IllegalStateException("SDK failure")
            fakeSdk.emit(DashscopeStreamingCallback.Error(failure))

            collectJob.join()

            assertTrue(result.last() is DashscopeStreamEvent.Error)
            val errorEvent = result.last() as DashscopeStreamEvent.Error
            assertEquals(failure, errorEvent.throwable)
        }

    @Test
    fun `cancels session when collector stops`() =
        scope.runTest {
            val request =
                DashscopeChatPayload(
                    model = "qwen-max",
                    input = ChatInput(listOf(ChatMessage.user("stop early"))),
                    parameters = ChatParameters.streaming(),
                )

            val firstChunk =
                launch {
                    client.streamChat(request).first()
                }

            fakeSdk.emit(DashscopeStreamingCallback.Chunk("only-one"))
            firstChunk.join()

            assertEquals(1, fakeSdk.cancelCount)
        }

    private class RecordingCredentialProvider : DashscopeCredentialProvider {
        var callCount = 0

        override suspend fun getCredentials(): DashscopeCredentials {
            callCount++
            return DashscopeCredentials(appKey = "app-key", token = "token-123")
        }
    }

    private class FakeDashscopeStreamingSdk : DashscopeStreamingSdk {
        var cancelCount = 0
        private var listener: DashscopeStreamingListener? = null

        override fun startChat(
            credentials: DashscopeCredentials,
            payload: DashscopeChatPayload,
            listener: DashscopeStreamingListener,
        ): DashscopeStreamingSession {
            this.listener = listener
            return object : DashscopeStreamingSession {
                override fun cancel() {
                    cancelCount++
                }
            }
        }

        fun emit(callback: DashscopeStreamingCallback) {
            val currentListener = listener ?: error("Chat not started")
            when (callback) {
                is DashscopeStreamingCallback.Chunk -> currentListener.onContentChunk(callback.markdown)
                is DashscopeStreamingCallback.Completed ->
                    currentListener.onCompleted(
                        totalTokens = callback.totalTokens,
                        requestId = callback.requestId,
                    )
                is DashscopeStreamingCallback.Error -> currentListener.onError(callback.throwable)
            }
        }
    }

    private sealed interface DashscopeStreamingCallback {
        data class Chunk(val markdown: String) : DashscopeStreamingCallback

        data class Completed(val totalTokens: Int, val requestId: String?) : DashscopeStreamingCallback

        data class Error(val throwable: Throwable) : DashscopeStreamingCallback
    }
}

package com.smartsales.data.network.dashscope

/**
 * Mock implementation of [DashscopeStreamingSdk] that doesn't depend on Alibaba SDK.
 * This is a temporary solution for compilation when Alibaba dependencies are not available.
 */
class OfficialDashscopeStreamingSdk(
    private val generationFactory: () -> Any = { Object() },
) : DashscopeStreamingSdk {
    override fun startChat(
        credentials: DashscopeCredentials,
        payload: DashscopeChatPayload,
        listener: DashscopeStreamingListener,
    ): DashscopeStreamingSession {
        // Mock implementation - simulate a simple chat response
        Thread {
            try {
                // Simulate streaming response with delays
                val mockResponse =
                    "This is a mock response from the AI assistant. " +
                        "The actual Alibaba DashScope SDK is not available in this build."

                // Send chunks with delays to simulate streaming
                val chunks = mockResponse.split(" ")
                for (chunk in chunks) {
                    Thread.sleep(100) // Simulate streaming delay
                    listener.onContentChunk(chunk + " ")
                }

                // Complete the response
                listener.onCompleted(50, "mock-request-id")
            } catch (e: Exception) {
                listener.onError(e)
            }
        }.start()

        return object : DashscopeStreamingSession {
            override fun cancel() {
                // Mock cancel implementation
                println("Mock streaming session cancelled")
            }
        }
    }

    private companion object {
        private const val DASHSCOPE_API_KEY_PROPERTY = "DASHSCOPE_API_KEY"
    }
}

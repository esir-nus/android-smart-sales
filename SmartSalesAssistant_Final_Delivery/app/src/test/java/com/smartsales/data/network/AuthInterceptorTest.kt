@file:Suppress("SpellCheckingInspection")

package com.smartsales.data.network

import com.smartsales.data.network.interceptor.DashscopeAuthInterceptor
import com.smartsales.data.network.interceptor.TingwuAuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.enqueue(MockResponse().setBody("ok"))
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `DashscopeAuthInterceptor adds bearer header`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(DashscopeAuthInterceptor("dash-key"))
            .build()

        client.newCall(newRequest()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer dash-key", recorded.getHeader(AiApiConfig.HEADER_AUTHORIZATION))
    }

    @Test
    fun `TingwuAuthInterceptor adds bearer header`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(TingwuAuthInterceptor("tingwu-key"))
            .build()

        client.newCall(newRequest()).execute().close()

        val recorded = server.takeRequest()
        assertEquals("Bearer tingwu-key", recorded.getHeader(AiApiConfig.HEADER_AUTHORIZATION))
    }

    private fun newRequest(): Request {
        return Request.Builder()
            .url(server.url("/test"))
            .build()
    }
}

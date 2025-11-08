@file:Suppress("SpellCheckingInspection")

package com.smartsales.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {

    @Test
    fun `getGadgetBaseUrl builds http url with default port`() {
        val url = ConnectivityApiConfig.buildBaseUrl("192.168.0.12")

        assertEquals(
            "http://192.168.0.12:${ConnectivityApiConfig.DEFAULT_PORT}/",
            url
        )
    }

    @Test
    fun `getGadgetBaseUrl allows custom port`() {
        val url = ConnectivityApiConfig.buildBaseUrl("10.0.0.5", port = 9000)

        assertEquals("http://10.0.0.5:9000/", url)
    }

    @Test
    fun `buildDashscopeAuth wraps key in bearer token`() {
        assertEquals("Bearer secret", AiApiConfig.buildBearerAuth("secret"))
    }

    @Test
    fun `buildTingwuAuth wraps key in bearer token`() {
        assertEquals("Bearer token123", AiApiConfig.buildBearerAuth("token123"))
    }
}

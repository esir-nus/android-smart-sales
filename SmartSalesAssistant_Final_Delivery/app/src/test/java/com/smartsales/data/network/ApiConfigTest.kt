package com.smartsales.data.network

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigTest {

    @Test
    fun `getGadgetBaseUrl builds http url with default port`() {
        val url = ApiConfig.getGadgetBaseUrl("192.168.0.12")

        assertEquals("http://192.168.0.12:${ApiConfig.GADGET_DEFAULT_PORT}/", url)
    }

    @Test
    fun `getGadgetBaseUrl allows custom port`() {
        val url = ApiConfig.getGadgetBaseUrl("10.0.0.5", port = 9000)

        assertEquals("http://10.0.0.5:9000/", url)
    }

    @Test
    fun `buildDashscopeAuth wraps key in bearer token`() {
        assertEquals("Bearer secret", ApiConfig.buildDashscopeAuth("secret"))
    }

    @Test
    fun `buildTingwuAuth wraps key in bearer token`() {
        assertEquals("Bearer token123", ApiConfig.buildTingwuAuth("token123"))
    }
}

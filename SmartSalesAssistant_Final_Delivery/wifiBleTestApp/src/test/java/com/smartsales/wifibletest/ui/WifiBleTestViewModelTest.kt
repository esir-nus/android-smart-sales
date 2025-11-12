package com.smartsales.wifibletest.ui

import com.smartsales.data.network.ConnectivityApiConfig
import org.junit.Assert.*
import org.junit.Test

class WifiBleTestViewModelTest {
    @Test
    fun `buildBaseUrl creates correct URL with default port`() {
        val url = ConnectivityApiConfig.buildBaseUrl("192.168.1.100")
        assertEquals("http://192.168.1.100:8000/", url)
    }

    @Test
    fun `buildBaseUrl creates correct URL with custom port`() {
        val url = ConnectivityApiConfig.buildBaseUrl("192.168.1.100", 9000)
        assertEquals("http://192.168.1.100:9000/", url)
    }

    @Test
    fun `verify server detection logic structure`() {
        // This test verifies that our server detection approach is sound
        // by testing the URL building logic that supports it

        // Test gadget API URL (default port)
        val gadgetUrl = ConnectivityApiConfig.buildBaseUrl("192.168.1.100")
        assertTrue(gadgetUrl.contains("8000"))

        // Test media server URL (custom port)
        val mediaUrl = ConnectivityApiConfig.buildBaseUrl("192.168.1.100", 34123)
        assertTrue(mediaUrl.contains("34123"))

        // Verify both use HTTP protocol
        assertTrue(gadgetUrl.startsWith("http://"))
        assertTrue(mediaUrl.startsWith("http://"))
    }
}

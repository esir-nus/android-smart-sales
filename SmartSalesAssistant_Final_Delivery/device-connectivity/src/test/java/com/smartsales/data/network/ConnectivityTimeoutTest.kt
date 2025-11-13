package com.smartsales.data.network

import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectivityTimeoutTest {
    @Test
    fun `wifi connect should fail fast before 5s to expose captive portals`() {
        assertTrue(
            "CONNECT_TIMEOUT_MS must be <= 5_000 to surface captive portal failures quickly.",
            ConnectivityApiConfig.CONNECT_TIMEOUT_MS <= 5_000,
        )
    }
}

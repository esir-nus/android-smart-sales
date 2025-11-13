package com.smartsales.business.bluetooth

import org.junit.Assert.assertTrue
import org.junit.Test

class BleBoundedScanTest {
    @Test
    fun `scan must be bounded by service filters`() {
        assertTrue(
            "SCAN_SERVICE_UUIDS should contain at least one UUID so scans stop once targets are filtered.",
            BleConstants.SCAN_SERVICE_UUIDS.isNotEmpty(),
        )
    }
}

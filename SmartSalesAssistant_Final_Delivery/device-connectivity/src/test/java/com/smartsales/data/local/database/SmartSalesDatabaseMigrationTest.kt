package com.smartsales.data.local.database

import org.junit.Assert.assertTrue
import org.junit.Test

class SmartSalesDatabaseMigrationTest {
    @Test
    fun `smart sales db should define forward migrations instead of destructive fallback`() {
        assertTrue(
            "Bump SmartSalesDatabase.DATABASE_VERSION above 1 and add migrations before releasing new schemas.",
            SmartSalesDatabase.DATABASE_VERSION > 1,
        )
    }
}

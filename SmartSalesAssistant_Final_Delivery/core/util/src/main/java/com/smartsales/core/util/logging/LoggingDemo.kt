package com.smartsales.core.util.logging

/**
 * Demo class to test logging functionality.
 * This will be removed in final implementation.
 */
object LoggingDemo {
    
    private const val TAG = "LoggingDemo"
    
    fun demonstrateRedaction() {
        // Test PII redaction
        val testMessages = listOf(
            "User email: john.doe@example.com contacted support",
            "Call customer at +1-234-567-8900 for follow-up",
            "Authorization: Bearer abc123def456 token provided",
            "WiFi password: MySecretPassword123 configured",
            "Normal log message without sensitive data"
        )
        
        testMessages.forEach { message ->
            Loggers.global.info(TAG, { "Original: $message" })
            Loggers.global.info(TAG, { "Redacted: ${Redactor.scrub(message)}" })
        }
    }
    
    fun demonstrateMetrics() {
        // Test telemetry
        com.smartsales.core.util.metrics.Telemetry.counter("demo_counter").increment()
        com.smartsales.core.util.metrics.Telemetry.counter("demo_counter").increment(5)
        
        val timer = com.smartsales.core.util.metrics.Telemetry.timer("demo_timer")
        val context = timer.start()
        Thread.sleep(100) // Simulate work
        context.stop()
        
        Loggers.global.debug(TAG, { "Metrics demo completed" })
    }
}
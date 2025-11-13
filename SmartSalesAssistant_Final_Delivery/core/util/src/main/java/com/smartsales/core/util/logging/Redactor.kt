package com.smartsales.core.util.logging

/**
 * PII scrubber for log messages.
 * Patterns:
 * - Emails: user@example.com
 * - Phones: +1-234-567-8900, (234) 567-8900, etc.
 * - Bearer tokens/API keys: Bearer abc123, abc-def-xyz
 * - Wi-Fi passwords: wpa_pass=secret123, password="secret"
 */
object Redactor {
    
    private val EMAIL_PATTERN = Regex(
        """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""",
        RegexOption.IGNORE_CASE
    )
    
    private val PHONE_PATTERN = Regex(
        """(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}"""
    )
    
    private val TOKEN_PATTERN = Regex(
        """(?i)(bearer|token|api[_-]?key|auth)[\s:=]+([a-z0-9_\-]{3,})"""
    )
    
    private val WIFI_PASSWORD_PATTERN = Regex(
        """(?i)(wpa_pass|password|psk)[\s:=]+["']?([\w!@#$%^&*()]{3,})["']?"""
    )
    
    /**
     * Scrub PII from message.
     * Returns redacted string with patterns replaced.
     */
    fun scrub(message: String): String {
        return message
            .replace(EMAIL_PATTERN, "<redacted:email>")
            .replace(PHONE_PATTERN, "<redacted:phone>")
            .replace(TOKEN_PATTERN) { match ->
                "${match.groupValues[1]}=<redacted:token>"
            }
            .replace(WIFI_PASSWORD_PATTERN) { match ->
                "${match.groupValues[1]}=<redacted:wifi>"
            }
    }
    
    /**
     * Data class for structured log events.
     */
    data class LogEvent(
        val level: String,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
}
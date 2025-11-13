package com.smartsales.core.util.logging

import org.junit.Test
import org.junit.Assert.*

class RedactorTest {
    
    @Test
    fun `scrub emails`() {
        val input = "User email: user@example.com and admin@test.org"
        val expected = "User email: <redacted:email> and <redacted:email>"
        assertEquals(expected, Redactor.scrub(input))
    }
    
    @Test
    fun `scrub phone numbers`() {
        val inputs = listOf(
            "Call me at +1-234-567-8900",
            "Phone: (234) 567-8900",
            "Contact: 234.567.8900",
            "Reach me at 234 567 8900"
        )
        
        inputs.forEach { input ->
            val result = Redactor.scrub(input)
            assertTrue("Phone not scrubbed in: $result", result.contains("<redacted:phone>"))
        }
    }
    
    @Test
    fun `scrub bearer tokens`() {
        val input = "Authorization: Bearer abc123def456"
        val expected = "Authorization: Bearer=<redacted:token>"
        assertEquals(expected, Redactor.scrub(input))
    }
    
    @Test
    fun `scrub api keys`() {
        val inputs = listOf(
            "api-key: xyz789",
            "api_key=secret123",
            "token: bearer-token-456",
            "auth=abc-def-ghi"
        )
        
        inputs.forEach { input ->
            val result = Redactor.scrub(input)
            assertTrue("Token not scrubbed in: $result", result.contains("<redacted:token>"))
        }
    }
    
    @Test
    fun `scrub wifi passwords`() {
        val inputs = listOf(
            "wpa_pass=secret123",
            "password=\"mySecret\"",
            "psk='wifi-password'",
            "wpa_pass: superSecret123!"
        )
        
        inputs.forEach { input ->
            val result = Redactor.scrub(input)
            assertTrue("WiFi password not scrubbed in: $result", result.contains("<redacted:wifi>"))
        }
    }
    
    @Test
    fun `no scrub for safe content`() {
        val input = "Normal log message with safe information"
        assertEquals(input, Redactor.scrub(input))
    }
    
    @Test
    fun `scrub multiple patterns in one message`() {
        val input = "User john@example.com with phone +1-234-567-8900 and token abc123 called"
        val result = Redactor.scrub(input)
        
        assertTrue("Email not scrubbed", result.contains("<redacted:email>"))
        assertTrue("Phone not scrubbed", result.contains("<redacted:phone>"))
        assertTrue("Token not scrubbed", result.contains("<redacted:token>"))
    }
}
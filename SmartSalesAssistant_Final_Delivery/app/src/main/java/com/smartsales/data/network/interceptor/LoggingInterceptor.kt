package com.smartsales.data.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Custom Logging Interceptor
 * 
 * Provides better formatted logs for debugging
 */
class CustomLoggingInterceptor(
    private val tag: String = "NetworkAPI"
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Log request
        Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        Log.d(tag, "→ ${request.method} ${request.url}")
        
        // Log headers (without sensitive data)
        request.headers.forEach { (name, value) ->
            if (!name.equals("Authorization", ignoreCase = true)) {
                Log.d(tag, "  $name: $value")
            } else {
                Log.d(tag, "  $name: Bearer [REDACTED]")
            }
        }
        
        // Log body for POST/PUT
        request.body?.let { body ->
            Log.d(tag, "  Body: ${body.contentType()}")
        }
        
        // Execute request
        val startTime = System.currentTimeMillis()
        val response = chain.proceed(request)
        val duration = System.currentTimeMillis() - startTime
        
        // Log response
        Log.d(tag, "← ${response.code} ${response.message} (${duration}ms)")
        
        if (!response.isSuccessful) {
            Log.e(tag, "  ERROR: ${response.code}")
        }
        
        Log.d(tag, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        return response
    }
}

/**
 * Get standard OkHttp logging interceptor
 */
fun createHttpLoggingInterceptor(level: HttpLoggingInterceptor.Level = HttpLoggingInterceptor.Level.BODY): HttpLoggingInterceptor {
    return HttpLoggingInterceptor().apply {
        setLevel(level)
    }
}
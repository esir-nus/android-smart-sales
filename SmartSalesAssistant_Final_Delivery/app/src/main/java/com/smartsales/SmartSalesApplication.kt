package com.smartsales

import android.app.Application
import android.util.Log
import com.smartsales.core.util.logging.AndroidLogger
import com.smartsales.core.util.logging.Loggers
import com.smartsales.core.util.metrics.Telemetry
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartSalesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Install Android logger with PII redaction
        Loggers.install(AndroidLogger)
        Loggers.info("SmartSalesApp", { "Application starting" })
        
        // Install global crash handler
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                // Log redacted crash info
                Loggers.error("Crash", { "Uncaught exception in ${thread.name}" }, throwable)
                
                // Increment crash counter
                Telemetry.global.counter("crash").increment()
                
                // Log additional details (redacted)
                Loggers.error("Crash", { "Exception type: ${throwable.javaClass.simpleName}" })
            } catch (e: Exception) {
                // Fallback to Android Log if our logger fails
                Log.e("CrashHandler", "Failed to log crash", e)
            } finally {
                // Call original handler
                originalHandler?.uncaughtException(thread, throwable)
            }
        }
        
        Loggers.debug("SmartSalesApp", { "Crash handler installed" })
    }
}

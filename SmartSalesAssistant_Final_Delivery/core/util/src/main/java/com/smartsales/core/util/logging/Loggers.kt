package com.smartsales.core.util.logging

/**
 * Global logger registry.
 * Default is [NoopLogger] to keep builds China-friendly.
 */
object Loggers {
    
    @JvmField
    var global: Logger = NoopLogger
    
    @JvmStatic
    fun install(logger: Logger) {
        global = logger
    }
}
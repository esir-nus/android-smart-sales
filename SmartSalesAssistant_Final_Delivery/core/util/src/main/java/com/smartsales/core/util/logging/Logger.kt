package com.smartsales.core.util.logging

/**
 * Vendor-neutral logging facade.
 * All messages are passed through [Redactor.scrub] before output.
 */
interface Logger {
    
    fun verbose(tag: String, msg: () -> String)
    fun debug(tag: String, msg: () -> String)
    fun info(tag: String, msg: () -> String)
    fun warn(tag: String, msg: () -> String, throwable: Throwable? = null)
    fun error(tag: String, msg: () -> String, throwable: Throwable? = null)
}
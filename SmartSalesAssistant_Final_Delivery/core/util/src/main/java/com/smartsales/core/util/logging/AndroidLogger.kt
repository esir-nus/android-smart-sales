package com.smartsales.core.util.logging

import android.util.Log

/**
 * Android-specific logger that wraps android.util.Log
 * and applies PII redaction via [Redactor.scrub].
 */
object AndroidLogger : Logger {
    
    override fun verbose(tag: String, msg: () -> String) {
        Log.v(tag, Redactor.scrub(msg()))
    }
    
    override fun debug(tag: String, msg: () -> String) {
        Log.d(tag, Redactor.scrub(msg()))
    }
    
    override fun info(tag: String, msg: () -> String) {
        Log.i(tag, Redactor.scrub(msg()))
    }
    
    override fun warn(tag: String, msg: () -> String, throwable: Throwable?) {
        val scrubbedMsg = Redactor.scrub(msg())
        if (throwable != null) {
            Log.w(tag, scrubbedMsg, throwable)
        } else {
            Log.w(tag, scrubbedMsg)
        }
    }
    
    override fun error(tag: String, msg: () -> String, throwable: Throwable?) {
        val scrubbedMsg = Redactor.scrub(msg())
        if (throwable != null) {
            Log.e(tag, scrubbedMsg, throwable)
        } else {
            Log.e(tag, scrubbedMsg)
        }
    }
}
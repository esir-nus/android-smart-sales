package com.smartsales.core.util.logging

/**
 * No-operation logger for tests and China-friendly builds.
 */
object NoopLogger : Logger {
    override fun verbose(tag: String, msg: () -> String) = Unit
    override fun debug(tag: String, msg: () -> String) = Unit
    override fun info(tag: String, msg: () -> String) = Unit
    override fun warn(tag: String, msg: () -> String, throwable: Throwable?) = Unit
    override fun error(tag: String, msg: () -> String, throwable: Throwable?) = Unit
}
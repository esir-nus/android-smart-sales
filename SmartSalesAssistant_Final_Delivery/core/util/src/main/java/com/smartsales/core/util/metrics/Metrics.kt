package com.smartsales.core.util.metrics

/**
 * Vendor-neutral metrics facade.
 * Default implementation is no-op to keep builds China-friendly.
 */
interface Metrics {
    
    fun counter(name: String): Counter
    fun timer(name: String): Timer
    fun gauge(name: String, value: Double)
    
    interface Counter {
        fun increment(amount: Long = 1)
        fun get(): Long
    }
    
    interface Timer {
        fun start(): TimerContext
        fun record(durationMs: Long)
    }
    
    interface TimerContext {
        fun stop()
    }
    
    /**
     * No-op implementation for China-friendly builds.
     */
    object Noop : Metrics {
        override fun counter(name: String) = NoopCounter
        override fun timer(name: String) = NoopTimer
        override fun gauge(name: String, value: Double) = Unit
        
        object NoopCounter : Counter {
            override fun increment(amount: Long) = Unit
            override fun get() = 0L
        }
        
        object NoopTimer : Timer {
            override fun start() = NoopTimerContext
            override fun record(durationMs: Long) = Unit
            
            object NoopTimerContext : TimerContext {
                override fun stop() = Unit
            }
        }
    }
}

/**
 * Global telemetry registry.
 */
object Telemetry {
    
    @JvmField
    var global: Metrics = Metrics.Noop
    
    @JvmStatic
    fun install(metrics: Metrics) {
        global = metrics
    }
    
    @JvmStatic
    fun counter(name: String) = global.counter(name)
    
    @JvmStatic
    fun timer(name: String) = global.timer(name)
}
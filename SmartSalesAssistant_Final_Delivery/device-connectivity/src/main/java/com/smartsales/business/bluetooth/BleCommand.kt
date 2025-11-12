package com.smartsales.business.bluetooth

/**
 * BLE Command Enum
 *
 * Defines all commands that can be sent to the device
 */
enum class BleCommand(val value: Byte, val description: String) {
    /**
     * Start audio recording
     */
    START_RECORDING(0x01, "Start Recording"),

    /**
     * Stop audio recording
     */
    STOP_RECORDING(0x02, "Stop Recording"),

    /**
     * Show image on device display
     */
    SHOW_IMAGE(0x03, "Show Image"),

    /**
     * Show text on device display
     */
    SHOW_TEXT(0x04, "Show Text"),

    /**
     * Clear device display
     */
    CLEAR_DISPLAY(0x05, "Clear Display"),

    /**
     * Request device status
     */
    REQUEST_STATUS(0x06, "Request Status"),

    /**
     * Sync files from device
     */
    START_FILE_SYNC(0x07, "Start File Sync"),

    /**
     * Stop file sync
     */
    STOP_FILE_SYNC(0x08, "Stop File Sync"),

    /**
     * Restart device
     */
    RESTART_DEVICE(0x09, "Restart Device"),

    /**
     * Enter sleep mode
     */
    ENTER_SLEEP_MODE(0x0A, "Enter Sleep Mode"),

    /**
     * Wake up device
     */
    WAKE_UP(0x0B, "Wake Up"),

    /**
     * Factory reset
     */
    FACTORY_RESET(0x0C, "Factory Reset"),
    ;

    /**
     * Convert command to byte array for sending
     */
    fun toByteArray(): ByteArray {
        return byteArrayOf(value)
    }

    /**
     * Convert command to byte array with parameter
     */
    fun toByteArrayWithParam(param: Byte): ByteArray {
        return byteArrayOf(value, param)
    }

    /**
     * Convert command to byte array with multiple parameters
     */
    fun toByteArrayWithParams(vararg params: Byte): ByteArray {
        return byteArrayOf(value, *params)
    }

    companion object {
        /**
         * Get command from byte value
         */
        fun fromByte(byte: Byte): BleCommand? {
            return values().find { it.value == byte }
        }

        /**
         * Check if byte is a valid command
         */
        fun isValidCommand(byte: Byte): Boolean {
            return fromByte(byte) != null
        }
    }

    /**
     * Check if command requires parameters
     */
    fun requiresParameters(): Boolean {
        return when (this) {
            SHOW_IMAGE, SHOW_TEXT -> true
            else -> false
        }
    }

    /**
     * Check if command is destructive (requires confirmation)
     */
    fun isDestructive(): Boolean {
        return when (this) {
            FACTORY_RESET, RESTART_DEVICE -> true
            else -> false
        }
    }

    /**
     * Get estimated execution time (milliseconds)
     */
    fun getEstimatedExecutionTime(): Long {
        return when (this) {
            START_RECORDING, STOP_RECORDING -> 500L
            SHOW_IMAGE, SHOW_TEXT, CLEAR_DISPLAY -> 200L
            REQUEST_STATUS -> 1000L
            START_FILE_SYNC -> 5000L
            RESTART_DEVICE -> 3000L
            FACTORY_RESET -> 10000L
            else -> 1000L
        }
    }
}

package com.smartsales.business.bluetooth

import android.bluetooth.BluetoothDevice

/**
 * BLE Connection State Sealed Class
 * 
 * Represents all possible connection states
 */
sealed class BleConnectionState {
    
    /**
     * Not connected to any device
     */
    object Disconnected : BleConnectionState() {
        override fun toString() = "Disconnected"
    }
    
    /**
     * Scanning for devices
     */
    data class Scanning(val devicesFound: Int = 0) : BleConnectionState() {
        override fun toString() = "Scanning ($devicesFound devices found)"
    }
    
    /**
     * Attempting to connect
     */
    data class Connecting(val device: BluetoothDevice) : BleConnectionState() {
        override fun toString() = "Connecting to ${device.name ?: device.address}"
    }
    
    /**
     * Connected but services not yet discovered
     */
    data class Connected(val device: BluetoothDevice) : BleConnectionState() {
        override fun toString() = "Connected to ${device.name ?: device.address}"
    }
    
    /**
     * Connected and services discovered - ready to use
     */
    data class Ready(val device: BluetoothDevice) : BleConnectionState() {
        override fun toString() = "Ready (${device.name ?: device.address})"
    }
    
    /**
     * Connection lost, attempting to reconnect
     */
    data class Reconnecting(
        val device: BluetoothDevice,
        val attempt: Int,
        val maxAttempts: Int
    ) : BleConnectionState() {
        override fun toString() = "Reconnecting ($attempt/$maxAttempts)"
    }
    
    /**
     * Error state
     */
    data class Error(
        val message: String,
        val exception: Exception? = null,
        val device: BluetoothDevice? = null
    ) : BleConnectionState() {
        override fun toString() = "Error: $message"
    }
    
    // ===== HELPER METHODS =====
    
    /**
     * Check if currently connected
     */
    fun isConnected(): Boolean {
        return this is Connected || this is Ready
    }
    
    /**
     * Check if ready for operations
     */
    fun isReady(): Boolean {
        return this is Ready
    }
    
    /**
     * Check if in error state
     */
    fun isError(): Boolean {
        return this is Error
    }
    
    /**
     * Check if scanning
     */
    fun isScanning(): Boolean {
        return this is Scanning
    }
    
    /**
     * Check if connecting or reconnecting
     */
    fun isConnecting(): Boolean {
        return this is Connecting || this is Reconnecting
    }
    
    /**
     * Get device if available
     */
    fun getDevice(): BluetoothDevice? {
        return when (this) {
            is Connecting -> device
            is Connected -> device
            is Ready -> device
            is Reconnecting -> device
            is Error -> device
            else -> null
        }
    }
    
    /**
     * Get user-friendly status message
     */
    fun getStatusMessage(): String {
        return when (this) {
            is Disconnected -> "Not connected"
            is Scanning -> "Searching for devices..."
            is Connecting -> "Connecting to ${device.name ?: "device"}..."
            is Connected -> "Connected"
            is Ready -> "Ready to use"
            is Reconnecting -> "Reconnecting (attempt $attempt of $maxAttempts)..."
            is Error -> message
        }
    }
    
    /**
     * Check if can send commands
     */
    fun canSendCommands(): Boolean {
        return this is Ready
    }
    
    /**
     * Check if can disconnect
     */
    fun canDisconnect(): Boolean {
        return this is Connected || this is Ready
    }
}

/**
 * BLE Error Types
 */
sealed class BleError(message: String, cause: Exception? = null) : Exception(message, cause) {
    
    class BluetoothNotEnabled : BleError("Bluetooth is not enabled")
    
    class PermissionDenied(permission: String) : BleError("Permission denied: $permission")
    
    class DeviceNotFound : BleError("Device not found")
    
    class ConnectionTimeout : BleError("Connection timeout")
    
    class ConnectionFailed(cause: Exception? = null) : BleError("Connection failed", cause)
    
    class ServiceNotFound : BleError("Required BLE service not found")
    
    class CharacteristicNotFound : BleError("Required characteristic not found")
    
    class WriteOperationFailed : BleError("Write operation failed")
    
    class ReadOperationFailed : BleError("Read operation failed")
    
    class NotificationEnableFailed : BleError("Failed to enable notifications")
    
    class UnexpectedDisconnection : BleError("Unexpected disconnection")
    
    class InvalidData(message: String) : BleError("Invalid data: $message")
}
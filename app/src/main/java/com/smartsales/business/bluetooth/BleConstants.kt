package com.smartsales.business.bluetooth

import java.nio.ByteBuffer
import java.util.*

/**
 * BLE Constants and Protocol Definitions
 * 
 * IMPORTANT: Replace these UUIDs with your actual hardware's UUIDs
 * You can find these in your hardware's BLE specification
 */
object BleConstants {
    
    // ===== SERVICE & CHARACTERISTIC UUIDs =====
    
    /**
     * Primary GATT Service UUID
     * This is the main service your device advertises
     */
    val SERVICE_UUID: UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
    
    /**
     * WiFi Configuration Characteristic UUID
     * Used to send WiFi credentials to the device
     */
    val CHAR_WIFI_CONFIG: UUID = UUID.fromString("00001235-0000-1000-8000-00805f9b34fb")
    
    /**
     * Command Characteristic UUID
     * Used to send control commands (start recording, show image, etc.)
     */
    val CHAR_COMMAND: UUID = UUID.fromString("00001236-0000-1000-8000-00805f9b34fb")
    
    /**
     * Device Status Characteristic UUID
     * Used to read device status (battery, WiFi connected, etc.)
     */
    val CHAR_DEVICE_STATUS: UUID = UUID.fromString("00001237-0000-1000-8000-00805f9b34fb")
    
    /**
     * Display Control Characteristic UUID
     * Used to send display commands (text/image)
     */
    val CHAR_DISPLAY_CONTROL: UUID = UUID.fromString("00001238-0000-1000-8000-00805f9b34fb")
    
    /**
     * Notification Characteristic UUID
     * Device sends notifications through this characteristic
     */
    val CHAR_NOTIFICATION: UUID = UUID.fromString("00001239-0000-1000-8000-00805f9b34fb")
    
    // ===== DESCRIPTOR UUIDs =====
    
    /**
     * Client Characteristic Configuration Descriptor
     * Used to enable notifications
     */
    val DESCRIPTOR_CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    
    // ===== SCAN SETTINGS =====
    
    /**
     * BLE scan timeout (milliseconds)
     */
    const val SCAN_TIMEOUT_MS = 10_000L // 10 seconds
    
    /**
     * Device name prefix for filtering
     * Only scan for devices starting with this prefix
     */
    const val DEVICE_NAME_PREFIX = "SmartSales"
    
    /**
     * Maximum number of devices to track during scan
     */
    const val MAX_SCAN_RESULTS = 50
    
    // ===== CONNECTION SETTINGS =====
    
    /**
     * Connection timeout (milliseconds)
     */
    const val CONNECTION_TIMEOUT_MS = 30_000L // 30 seconds
    
    /**
     * Auto-reconnect attempts
     */
    const val MAX_RECONNECT_ATTEMPTS = 3
    
    /**
     * Reconnect delay (milliseconds)
     */
    const val RECONNECT_DELAY_MS = 2_000L // 2 seconds
    
    /**
     * GATT operation timeout (milliseconds)
     */
    const val GATT_OPERATION_TIMEOUT_MS = 5_000L // 5 seconds
    
    // ===== PACKET SIZES =====
    
    /**
     * Maximum BLE packet size (bytes)
     * Most BLE devices support 20 bytes MTU by default
     */
    const val MAX_PACKET_SIZE = 20
    
    /**
     * Maximum SSID length
     */
    const val MAX_SSID_LENGTH = 32
    
    /**
     * Maximum password length
     */
    const val MAX_PASSWORD_LENGTH = 63
    
    /**
     * Maximum display text length
     */
    const val MAX_DISPLAY_TEXT_LENGTH = 100
    
    /**
     * Maximum image filename length
     */
    const val MAX_IMAGE_NAME_LENGTH = 50
    
    // ===== PROTOCOL DEFINITIONS =====
    
    /**
     * Encode WiFi configuration into BLE packet
     * 
     * Packet format:
     * [SSID_LENGTH(1)][SSID_BYTES][PASSWORD_LENGTH(1)][PASSWORD_BYTES]
     * 
     * @param ssid WiFi network name
     * @param password WiFi password
     * @return Byte array ready to send
     */
    fun encodeWifiConfig(ssid: String, password: String): ByteArray {
        val ssidTrimmed = ssid.take(MAX_SSID_LENGTH)
        val passwordTrimmed = password.take(MAX_PASSWORD_LENGTH)
        
        val ssidBytes = ssidTrimmed.toByteArray(Charsets.UTF_8)
        val passwordBytes = passwordTrimmed.toByteArray(Charsets.UTF_8)
        
        return ByteBuffer.allocate(2 + ssidBytes.size + passwordBytes.size)
            .put(ssidBytes.size.toByte())
            .put(ssidBytes)
            .put(passwordBytes.size.toByte())
            .put(passwordBytes)
            .array()
    }
    
    /**
     * Encode display text command
     * 
     * Packet format:
     * [COMMAND_TYPE(1)][TEXT_LENGTH(1)][TEXT_BYTES]
     * 
     * @param text Text to display
     * @return Byte array ready to send
     */
    fun encodeDisplayText(text: String): ByteArray {
        val textTrimmed = text.take(MAX_DISPLAY_TEXT_LENGTH)
        val textBytes = textTrimmed.toByteArray(Charsets.UTF_8)
        
        return ByteBuffer.allocate(2 + textBytes.size)
            .put(0x04) // SHOW_TEXT command
            .put(textBytes.size.toByte())
            .put(textBytes)
            .array()
    }
    
    /**
     * Encode display image command
     * 
     * Packet format:
     * [COMMAND_TYPE(1)][FILENAME_LENGTH(1)][FILENAME_BYTES]
     * 
     * @param fileName Image filename (must exist on device)
     * @return Byte array ready to send
     */
    fun encodeDisplayImage(fileName: String): ByteArray {
        val fileNameTrimmed = fileName.take(MAX_IMAGE_NAME_LENGTH)
        val fileNameBytes = fileNameTrimmed.toByteArray(Charsets.UTF_8)
        
        return ByteBuffer.allocate(2 + fileNameBytes.size)
            .put(0x03) // SHOW_IMAGE command
            .put(fileNameBytes.size.toByte())
            .put(fileNameBytes)
            .array()
    }
    
    /**
     * Decode device status response
     * 
     * Status format:
     * [BATTERY_LEVEL(1)][WIFI_CONNECTED(1)][RECORDING_STATE(1)][RESERVED(17)]
     * 
     * @param data Status data from device
     * @return DeviceStatus object
     */
    fun decodeDeviceStatus(data: ByteArray): DeviceStatus {
        if (data.size < 3) {
            return DeviceStatus.unknown()
        }
        
        return DeviceStatus(
            batteryLevel = data[0].toInt() and 0xFF,
            isWifiConnected = data[1].toInt() == 1,
            isRecording = data[2].toInt() == 1
        )
    }
    
    /**
     * Split large data into BLE-sized chunks
     * 
     * @param data Data to split
     * @param chunkSize Chunk size (default: MAX_PACKET_SIZE)
     * @return List of byte arrays
     */
    fun chunkData(data: ByteArray, chunkSize: Int = MAX_PACKET_SIZE): List<ByteArray> {
        return data.toList().chunked(chunkSize).map { it.toByteArray() }
    }
    
    /**
     * Calculate checksum for data integrity
     * 
     * @param data Data to checksum
     * @return Checksum byte
     */
    fun calculateChecksum(data: ByteArray): Byte {
        var sum = 0
        data.forEach { sum += it.toInt() and 0xFF }
        return (sum and 0xFF).toByte()
    }
    
    /**
     * Verify checksum
     * 
     * @param data Data with checksum as last byte
     * @return True if checksum valid
     */
    fun verifyChecksum(data: ByteArray): Boolean {
        if (data.isEmpty()) return false
        
        val receivedChecksum = data.last()
        val calculatedChecksum = calculateChecksum(data.copyOfRange(0, data.size - 1))
        
        return receivedChecksum == calculatedChecksum
    }
}

/**
 * Device status data class
 */
data class DeviceStatus(
    val batteryLevel: Int,      // 0-100 percentage
    val isWifiConnected: Boolean,
    val isRecording: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun unknown(): DeviceStatus {
            return DeviceStatus(
                batteryLevel = 0,
                isWifiConnected = false,
                isRecording = false
            )
        }
    }
    
    /**
     * Check if battery is low (<20%)
     */
    val isBatteryLow: Boolean
        get() = batteryLevel < 20
    
    /**
     * Check if battery is critical (<10%)
     */
    val isBatteryCritical: Boolean
        get() = batteryLevel < 10
    
    /**
     * Get battery status text
     */
    fun getBatteryStatusText(): String {
        return when {
            batteryLevel >= 80 -> "Full"
            batteryLevel >= 50 -> "Good"
            batteryLevel >= 20 -> "Medium"
            batteryLevel >= 10 -> "Low"
            else -> "Critical"
        }
    }
    
    /**
     * Get device status summary
     */
    fun getSummary(): String {
        return buildString {
            append("Battery: $batteryLevel% (${getBatteryStatusText()})")
            append(", WiFi: ${if (isWifiConnected) "Connected" else "Disconnected"}")
            append(", Recording: ${if (isRecording) "Active" else "Inactive"}")
        }
    }
}
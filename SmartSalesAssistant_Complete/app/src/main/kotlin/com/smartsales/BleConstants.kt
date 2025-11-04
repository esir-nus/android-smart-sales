package com.smartsales.data.bluetooth

import java.util.UUID

/**
 * BLE service and characteristic UUIDs for Smart Sales Gadget
 */
object BleConstants {
    
    // Custom service UUID for Smart Sales Gadget
    val SERVICE_UUID: UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
    
    // WiFi configuration characteristic (write SSID and password)
    val CHAR_WIFI_CONFIG: UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")
    
    // Device status characteristic (read device information)
    val CHAR_DEVICE_STATUS: UUID = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb")
    
    // Command characteristic (send control commands)
    val CHAR_COMMAND: UUID = UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb")
    
    /**
     * Encode WiFi configuration to byte array
     * Format: SSID|PASSWORD (using | as separator, UTF-8 encoding)
     */
    fun encodeWifiConfig(ssid: String, password: String): ByteArray {
        return "$ssid|$password".toByteArray(Charsets.UTF_8)
    }
    
    /**
     * Decode WiFi configuration from byte array
     */
    fun decodeWifiConfig(data: ByteArray): Pair<String, String>? {
        val config = String(data, Charsets.UTF_8)
        val parts = config.split("|")
        return if (parts.size == 2) Pair(parts[0], parts[1]) else null
    }
}

/**
 * BLE command types for device control
 */
enum class BleCommand(val value: Byte) {
    SYNC_FILES(0x01),           // Sync file list
    UPDATE_IMAGE(0x02),         // Update display image
    UPDATE_TEXT(0x03),          // Update display text
    GET_DEVICE_INFO(0x04),      // Get device information
    REBOOT(0x05);               // Reboot device
    
    fun toByteArray(): ByteArray = byteArrayOf(value)
}

/**
 * BLE connection states
 */
sealed class BleConnectionState {
    object Disconnected : BleConnectionState()
    data class Connected(val deviceAddress: String) : BleConnectionState()
    data class ServicesDiscovered(val deviceAddress: String) : BleConnectionState()
    data class Error(val message: String) : BleConnectionState()
}

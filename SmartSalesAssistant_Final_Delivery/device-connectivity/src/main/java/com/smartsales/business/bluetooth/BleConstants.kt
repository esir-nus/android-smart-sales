package com.smartsales.business.bluetooth

import java.nio.ByteBuffer
import java.util.UUID

data class BleServiceProfile(
    val name: String,
    val serviceUuid: UUID,
    val wifiConfigCharacteristic: UUID?,
    val commandCharacteristic: UUID?,
    val displayControlCharacteristic: UUID?,
    val notificationCharacteristic: UUID?,
    val deviceStatusCharacteristic: UUID?
)

/**
 * BLE Constants and Protocol Definitions
 *
 * IMPORTANT: Replace these UUIDs with your actual hardware's UUIDs
 * You can find these in your hardware's BLE specification
 */
object BleConstants {

    // ===== SERVICE PROFILES =====

    private val LEGACY_PROFILE = BleServiceProfile(
        name = "SmartSales",
        serviceUuid = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb"),
        wifiConfigCharacteristic = UUID.fromString("00001235-0000-1000-8000-00805f9b34fb"),
        commandCharacteristic = UUID.fromString("00001236-0000-1000-8000-00805f9b34fb"),
        displayControlCharacteristic = UUID.fromString("00001238-0000-1000-8000-00805f9b34fb"),
        notificationCharacteristic = UUID.fromString("00001239-0000-1000-8000-00805f9b34fb"),
        deviceStatusCharacteristic = UUID.fromString("00001237-0000-1000-8000-00805f9b34fb")
    )

    private val UART_PROFILE = BleServiceProfile(
        name = "SPP BLE UART",
        serviceUuid = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"),
        wifiConfigCharacteristic = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
        commandCharacteristic = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
        displayControlCharacteristic = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E"),
        notificationCharacteristic = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E"),
        deviceStatusCharacteristic = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
    )

    private val HM10_PROFILE = BleServiceProfile(
        name = "HM-10 UART",
        serviceUuid = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB"),
        wifiConfigCharacteristic = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"),
        commandCharacteristic = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"),
        displayControlCharacteristic = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"),
        notificationCharacteristic = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB"),
        deviceStatusCharacteristic = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
    )

    val SUPPORTED_SERVICE_PROFILES: List<BleServiceProfile> = listOf(
        LEGACY_PROFILE,
        UART_PROFILE,
        HM10_PROFILE
    )

    /**
     * Service UUIDs we allow during scanning. Keep empty so we can discover
     * devices that do not advertise their primary service UUID (many UART modules).
     */
    val SCAN_SERVICE_UUIDS: List<UUID> = emptyList()

    const val NETWORK_QUERY_COMMAND = "wifi#address#ip#name"
    const val NETWORK_RESPONSE_PREFIX = "wifi#address#"
    const val TARGET_DEVICE_NAME = "BT311"
    
    // ===== DESCRIPTOR UUIDs =====
    
    /**
     * Client Characteristic Configuration Descriptor
     * Used to enable notifications
     */
    val DESCRIPTOR_CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    
    // ===== SCAN SETTINGS =====
    
    /**
     * Duration of each active scan window (milliseconds)
     */
    const val SCAN_WINDOW_MS = 6_000L

    /**
     * Delay between scan retries when target device is not found (milliseconds)
     */
    const val SCAN_RETRY_DELAY_MS = 2_000L
    
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
     * Default MTU size (bytes) negotiated by Android before requests
     */
    const val DEFAULT_MTU = 23

    /**
     * Preferred MTU we attempt to request from the gadget
     */
    const val PREFERRED_MTU = 185

    /**
     * MTU overhead for ATT protocol
     */
    const val GATT_MTU_OVERHEAD = 3

    /**
     * Default payload size when MTU negotiation is unavailable
     */
    const val DEFAULT_CHUNK_SIZE = DEFAULT_MTU - GATT_MTU_OVERHEAD

    /**
     * Delay between chunked writes (milliseconds)
     */
    const val CHUNK_WRITE_DELAY_MS = 40L
    
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
     * Encode WiFi configuration into JSON payload expected by gadget firmware.
     * 
     * @param ssid WiFi network name
     * @param password WiFi password
     * @return Byte array ready to send over BLE
     */
    fun encodeWifiConfig(ssid: String, password: String): ByteArray {
        val truncatedSsid = ssid.take(MAX_SSID_LENGTH)
        val truncatedPassword = password.take(MAX_PASSWORD_LENGTH)
        val payload = buildString {
            append("{\"ssid\":\"")
            append(escapeJsonValue(truncatedSsid))
            append("\",\"password\":\"")
            append(escapeJsonValue(truncatedPassword))
            append("\"}")
        }

        return payload.toByteArray(Charsets.UTF_8)
    }

    private fun escapeJsonValue(value: String): String {
        val builder = StringBuilder(value.length)
        value.forEach { ch ->
            when (ch) {
                '\"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                else -> {
                    if (ch < ' ') {
                        builder.append("\\u")
                        builder.append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        builder.append(ch)
                    }
                }
            }
        }
        return builder.toString()
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
     * @param chunkSize Chunk size (default: DEFAULT_CHUNK_SIZE)
     * @return List of byte arrays
     */
    fun chunkData(data: ByteArray, chunkSize: Int = DEFAULT_CHUNK_SIZE): List<ByteArray> {
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

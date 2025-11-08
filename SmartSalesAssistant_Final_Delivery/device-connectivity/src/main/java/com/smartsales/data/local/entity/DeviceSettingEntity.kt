package com.smartsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*

/**
 * Device Setting Entity - Stores BLE device configurations
 *
 * Features:
 * - BLE device pairing info
 * - Display state tracking (image/text)
 * - Connection history
 * - Device identification
 */
@Entity(tableName = "device_settings")
data class DeviceSettingEntity(
    @PrimaryKey
    val deviceId: String,            // BLE MAC address (e.g., "00:11:22:33:44:55")

    val deviceName: String,          // User-friendly device name
    val lastConnected: Long,         // Last connection timestamp (millis)
    val currentImage: String? = null, // Current displayed image filename
    val currentText: String? = null,  // Current displayed text
    val isPaired: Boolean = true      // Whether device is currently paired
) {

    companion object {
        // Device name limits
        const val MAX_DEVICE_NAME_LENGTH = 50
        const val MAX_TEXT_LENGTH = 500
        const val MAX_IMAGE_NAME_LENGTH = 255

        // Connection timeout
        const val CONNECTION_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutes

        // Default values
        const val DEFAULT_DEVICE_NAME = "Smart Sales Gadget"

        /**
         * Create new device setting
         */
        fun create(
            deviceId: String,
            deviceName: String = DEFAULT_DEVICE_NAME,
            isPaired: Boolean = true
        ): DeviceSettingEntity {
            return DeviceSettingEntity(
                deviceId = formatMacAddress(deviceId),
                deviceName = deviceName.take(MAX_DEVICE_NAME_LENGTH),
                lastConnected = System.currentTimeMillis(),
                currentImage = null,
                currentText = null,
                isPaired = isPaired
            )
        }

        /**
         * Format MAC address to standard format
         * Converts various formats to "XX:XX:XX:XX:XX:XX"
         */
        fun formatMacAddress(address: String): String {
            val cleaned = address.replace(Regex("[^0-9A-Fa-f]"), "")

            return if (cleaned.length == 12) {
                cleaned.chunked(2).joinToString(":")
            } else {
                address // Return as-is if not valid length
            }
        }

        /**
         * Validate MAC address format
         */
        fun isValidMacAddress(address: String): Boolean {
            val macRegex = Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$")
            return macRegex.matches(address)
        }

        /**
         * Generate device name from MAC address
         */
        fun generateDeviceNameFromMac(macAddress: String): String {
            val lastBytes = macAddress.split(":").takeLast(2).joinToString("")
            return "Gadget-$lastBytes"
        }
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Check if device is currently connected (connected within timeout)
     */
    val isConnected: Boolean
        get() = isPaired &&
                (System.currentTimeMillis() - lastConnected) < CONNECTION_TIMEOUT_MS

    /**
     * Check if connection is stale (no connection in 24+ hours)
     */
    val isConnectionStale: Boolean
        get() = System.currentTimeMillis() - lastConnected > 24 * 60 * 60 * 1000

    /**
     * Check if device has active display content
     */
    val hasActiveDisplay: Boolean
        get() = currentImage != null || currentText != null

    /**
     * Check if displaying image
     */
    val isDisplayingImage: Boolean
        get() = currentImage != null

    /**
     * Check if displaying text
     */
    val isDisplayingText: Boolean
        get() = currentText != null

    /**
     * Get connection age in seconds
     */
    val connectionAgeSeconds: Long
        get() = (System.currentTimeMillis() - lastConnected) / 1000

    /**
     * Get connection age in minutes
     */
    val connectionAgeMinutes: Long
        get() = connectionAgeSeconds / 60

    /**
     * Get connection age in hours
     */
    val connectionAgeHours: Long
        get() = connectionAgeMinutes / 60

    /**
     * Get short device ID (last 4 characters)
     */
    val shortDeviceId: String
        get() = deviceId.replace(":", "").takeLast(4).uppercase()

    /**
     * Get manufacturer from MAC address (first 3 bytes - OUI)
     */
    val manufacturerOui: String
        get() = deviceId.split(":").take(3).joinToString(":")

    /**
     * Get device display name with ID
     */
    val displayNameWithId: String
        get() = "$deviceName ($shortDeviceId)"

    // ===== STATUS METHODS =====

    /**
     * Get connection status
     */
    fun getConnectionStatus(): ConnectionStatus {
        return when {
            !isPaired -> ConnectionStatus.UNPAIRED
            isConnected -> ConnectionStatus.CONNECTED
            connectionAgeMinutes < 30 -> ConnectionStatus.RECENTLY_CONNECTED
            isConnectionStale -> ConnectionStatus.STALE
            else -> ConnectionStatus.DISCONNECTED
        }
    }

    /**
     * Get display status
     */
    fun getDisplayStatus(): DisplayStatus {
        return when {
            currentImage != null && currentText != null -> DisplayStatus.BOTH
            currentImage != null -> DisplayStatus.IMAGE_ONLY
            currentText != null -> DisplayStatus.TEXT_ONLY
            else -> DisplayStatus.CLEAR
        }
    }

    // ===== FORMATTING METHODS =====

    /**
     * Format last connected timestamp
     */
    fun getFormattedLastConnected(pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(lastConnected))
    }

    /**
     * Get relative connection time
     */
    fun getRelativeConnectionTime(): String {
        val seconds = connectionAgeSeconds

        return when {
            seconds < 60 -> "Just now"
            seconds < 3600 -> "${seconds / 60}m ago"
            seconds < 86400 -> "${seconds / 3600}h ago"
            seconds < 7 * 86400 -> "${seconds / 86400}d ago"
            else -> getFormattedLastConnected("MMM dd")
        }
    }

    /**
     * Get connection status text
     */
    fun getConnectionStatusText(): String {
        return when (getConnectionStatus()) {
            ConnectionStatus.CONNECTED -> "Connected"
            ConnectionStatus.RECENTLY_CONNECTED -> "Recently connected"
            ConnectionStatus.DISCONNECTED -> "Disconnected"
            ConnectionStatus.STALE -> "Connection stale"
            ConnectionStatus.UNPAIRED -> "Not paired"
        }
    }

    /**
     * Get display content summary
     */
    fun getDisplaySummary(): String {
        return when {
            currentImage != null && currentText != null -> {
                val preview = currentText.take(20)
                "Image: $currentImage, Text: $preview..."
            }
            currentImage != null -> "Image: $currentImage"
            currentText != null -> {
                val preview = currentText.take(50)
                "Text: $preview${if (currentText.length > 50) "..." else ""}"
            }
            else -> "No display content"
        }
    }

    /**
     * Get MAC address without colons
     */
    fun getMacAddressRaw(): String {
        return deviceId.replace(":", "")
    }

    /**
     * Get MAC address with custom separator
     */
    fun getMacAddressFormatted(separator: String = "-"): String {
        return deviceId.replace(":", separator)
    }

    // ===== VALIDATION METHODS =====

    /**
     * Validate device setting
     */
    fun isValid(): Boolean {
        return isValidMacAddress(deviceId) &&
                deviceName.isNotBlank() &&
                deviceName.length <= MAX_DEVICE_NAME_LENGTH &&
                (currentText == null || currentText.length <= MAX_TEXT_LENGTH) &&
                (currentImage == null || currentImage.length <= MAX_IMAGE_NAME_LENGTH)
    }

    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()

        if (!isValidMacAddress(deviceId)) {
            errors.add("Invalid MAC address format: $deviceId")
        }
        if (deviceName.isBlank()) {
            errors.add("Device name cannot be empty")
        }
        if (deviceName.length > MAX_DEVICE_NAME_LENGTH) {
            errors.add("Device name too long (max $MAX_DEVICE_NAME_LENGTH)")
        }
        if (currentText != null && currentText.length > MAX_TEXT_LENGTH) {
            errors.add("Display text too long (max $MAX_TEXT_LENGTH)")
        }
        if (currentImage != null && currentImage.length > MAX_IMAGE_NAME_LENGTH) {
            errors.add("Image filename too long (max $MAX_IMAGE_NAME_LENGTH)")
        }

        return errors
    }

    // ===== HELPER METHODS =====

    /**
     * Update connection timestamp
     */
    fun updateConnection(): DeviceSettingEntity {
        return copy(lastConnected = System.currentTimeMillis())
    }

    /**
     * Pair device
     */
    fun pair(): DeviceSettingEntity {
        return copy(
            isPaired = true,
            lastConnected = System.currentTimeMillis()
        )
    }

    /**
     * Unpair device
     */
    fun unpair(): DeviceSettingEntity {
        return copy(isPaired = false)
    }

    /**
     * Set display image
     */
    fun setDisplayImage(imageName: String?): DeviceSettingEntity {
        return copy(
            currentImage = imageName?.take(MAX_IMAGE_NAME_LENGTH),
            lastConnected = System.currentTimeMillis()
        )
    }

    /**
     * Set display text
     */
    fun setDisplayText(text: String?): DeviceSettingEntity {
        return copy(
            currentText = text?.take(MAX_TEXT_LENGTH),
            lastConnected = System.currentTimeMillis()
        )
    }

    /**
     * Clear display
     */
    fun clearDisplay(): DeviceSettingEntity {
        return copy(
            currentImage = null,
            currentText = null,
            lastConnected = System.currentTimeMillis()
        )
    }

    /**
     * Rename device
     */
    fun rename(newName: String): DeviceSettingEntity {
        return copy(deviceName = newName.take(MAX_DEVICE_NAME_LENGTH))
    }

    /**
     * Check if this device matches a MAC address
     */
    fun matchesMacAddress(address: String): Boolean {
        val formattedAddress = formatMacAddress(address)
        return deviceId.equals(formattedAddress, ignoreCase = true)
    }
}

/**
 * Connection status enum
 */
enum class ConnectionStatus {
    CONNECTED,
    RECENTLY_CONNECTED,
    DISCONNECTED,
    STALE,
    UNPAIRED
}

/**
 * Display status enum
 */
enum class DisplayStatus {
    CLEAR,
    IMAGE_ONLY,
    TEXT_ONLY,
    BOTH
}

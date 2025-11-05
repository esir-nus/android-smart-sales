package com.smartsales.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * WiFi Configuration Entity - Stores WiFi credentials
 *
 * Features:
 * - SSID and password storage
 * - Default configuration support
 * - Usage tracking
 * - Password encryption (placeholder - implement proper encryption)
 */
@Entity(tableName = "wifi_configs")
data class WifiConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val ssid: String,                // WiFi network name
    val password: String,            // WiFi password (should be encrypted)
    val isDefault: Boolean = false,  // Is this the default config
    val lastUsed: Long,              // Last used timestamp (millis)
    val addedAt: Long                // Added timestamp (millis)
) {

    companion object {
        // Validation constants
        const val MAX_SSID_LENGTH = 32
        const val MAX_PASSWORD_LENGTH = 63
        const val MIN_PASSWORD_LENGTH = 8

        // Encryption key (IMPORTANT: Use proper key management in production!)
        private const val ENCRYPTION_KEY = "SmartSales2024!!" // 16 chars for AES-128

        /**
         * Create new WiFi config
         */
        fun create(
            ssid: String,
            password: String,
            isDefault: Boolean = false
        ): WifiConfigEntity {
            val now = System.currentTimeMillis()
            return WifiConfigEntity(
                ssid = ssid.take(MAX_SSID_LENGTH),
                password = encryptPassword(password),
                isDefault = isDefault,
                lastUsed = now,
                addedAt = now
            )
        }

        /**
         * Encrypt password (basic implementation - use proper encryption in production)
         */
        private fun encryptPassword(password: String): String {
            return try {
                val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.ENCRYPT_MODE, key)
                val encrypted = cipher.doFinal(password.toByteArray())
                Base64.encodeToString(encrypted, Base64.DEFAULT)
            } catch (e: Exception) {
                // Fallback: store as-is (NOT secure!)
                password
            }
        }

        /**
         * Decrypt password
         */
        fun decryptPassword(encryptedPassword: String): String {
            return try {
                val key = SecretKeySpec(ENCRYPTION_KEY.toByteArray(), "AES")
                val cipher = Cipher.getInstance("AES")
                cipher.init(Cipher.DECRYPT_MODE, key)
                val decoded = Base64.decode(encryptedPassword, Base64.DEFAULT)
                val decrypted = cipher.doFinal(decoded)
                String(decrypted)
            } catch (e: Exception) {
                // Fallback: return as-is
                encryptedPassword
            }
        }

        /**
         * Validate SSID format
         */
        fun isValidSsid(ssid: String): Boolean {
            return ssid.isNotBlank() &&
                    ssid.length <= MAX_SSID_LENGTH &&
                    ssid.all { it.isLetterOrDigit() || it in "-_ " }
        }

        /**
         * Validate password format
         */
        fun isValidPassword(password: String): Boolean {
            return password.length >= MIN_PASSWORD_LENGTH &&
                    password.length <= MAX_PASSWORD_LENGTH
        }
    }

    // ===== COMPUTED PROPERTIES =====

    /**
     * Get decrypted password
     */
    val decryptedPassword: String
        get() = decryptPassword(password)

    /**
     * Check if recently used (within 24 hours)
     */
    val isRecentlyUsed: Boolean
        get() = System.currentTimeMillis() - lastUsed < 24 * 60 * 60 * 1000

    /**
     * Check if stale (not used in 30+ days)
     */
    val isStale: Boolean
        get() = System.currentTimeMillis() - lastUsed > 30 * 24 * 60 * 60 * 1000L

    /**
     * Get age in days
     */
    val ageInDays: Int
        get() = ((System.currentTimeMillis() - addedAt) / (24 * 60 * 60 * 1000)).toInt()

    /**
     * Get days since last used
     */
    val daysSinceLastUsed: Int
        get() = ((System.currentTimeMillis() - lastUsed) / (24 * 60 * 60 * 1000)).toInt()

    /**
     * Get masked SSID (for display)
     */
    val maskedSsid: String
        get() = if (ssid.length <= 4) {
            ssid
        } else {
            ssid.take(2) + "*".repeat(ssid.length - 4) + ssid.takeLast(2)
        }

    /**
     * Get password strength indicator
     */
    val passwordStrength: PasswordStrength
        get() = calculatePasswordStrength(decryptedPassword)

    // ===== FORMATTING METHODS =====

    /**
     * Format last used date
     */
    fun getFormattedLastUsed(pattern: String = "yyyy-MM-dd HH:mm"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(lastUsed))
    }

    /**
     * Format added date
     */
    fun getFormattedAddedDate(pattern: String = "yyyy-MM-dd"): String {
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(addedAt))
    }

    /**
     * Get relative last used time
     */
    fun getRelativeLastUsedTime(): String {
        val days = daysSinceLastUsed
        return when {
            days == 0 -> "Today"
            days == 1 -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 30 -> "${days / 7} weeks ago"
            else -> "${days / 30} months ago"
        }
    }

    /**
     * Get display name with status
     */
    fun getDisplayName(): String {
        return if (isDefault) {
            "$ssid (Default)"
        } else {
            ssid
        }
    }

    // ===== VALIDATION METHODS =====

    /**
     * Validate configuration
     */
    fun isValid(): Boolean {
        return isValidSsid(ssid) &&
                isValidPassword(decryptedPassword)
    }

    /**
     * Get validation errors
     */
    fun getValidationErrors(): List<String> {
        val errors = mutableListOf<String>()
        val decrypted = decryptedPassword

        if (ssid.isBlank()) {
            errors.add("SSID cannot be empty")
        }
        if (ssid.length > MAX_SSID_LENGTH) {
            errors.add("SSID too long (max $MAX_SSID_LENGTH characters)")
        }
        if (decrypted.length < MIN_PASSWORD_LENGTH) {
            errors.add("Password too short (min $MIN_PASSWORD_LENGTH characters)")
        }
        if (decrypted.length > MAX_PASSWORD_LENGTH) {
            errors.add("Password too long (max $MAX_PASSWORD_LENGTH characters)")
        }

        return errors
    }

    // ===== HELPER METHODS =====

    /**
     * Mark as used (update timestamp)
     */
    fun markAsUsed(): WifiConfigEntity {
        return copy(lastUsed = System.currentTimeMillis())
    }

    /**
     * Set as default
     */
    fun setAsDefault(): WifiConfigEntity {
        return copy(isDefault = true, lastUsed = System.currentTimeMillis())
    }

    /**
     * Remove default status
     */
    fun removeDefault(): WifiConfigEntity {
        return copy(isDefault = false)
    }

    /**
     * Update password
     */
    fun withNewPassword(newPassword: String): WifiConfigEntity {
        return copy(
            password = encryptPassword(newPassword),
            lastUsed = System.currentTimeMillis()
        )
    }

    /**
     * Calculate password strength
     */
    private fun calculatePasswordStrength(pwd: String): PasswordStrength {
        var score = 0

        if (pwd.length >= 8) score++
        if (pwd.length >= 12) score++
        if (pwd.any { it.isDigit() }) score++
        if (pwd.any { it.isUpperCase() }) score++
        if (pwd.any { it.isLowerCase() }) score++
        if (pwd.any { !it.isLetterOrDigit() }) score++

        return when {
            score >= 5 -> PasswordStrength.STRONG
            score >= 3 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}

/**
 * Password strength enum
 */
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}
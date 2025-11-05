package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.DeviceSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceSettingDao {
    
    // ===== QUERY OPERATIONS =====
    
    /**
     * Get currently paired device
     */
    @Query("SELECT * FROM device_settings WHERE isPaired = 1 LIMIT 1")
    suspend fun getCurrentDevice(): DeviceSettingEntity?
    
    /**
     * Get current device as Flow
     */
    @Query("SELECT * FROM device_settings WHERE isPaired = 1 LIMIT 1")
    fun getCurrentDeviceFlow(): Flow<DeviceSettingEntity?>
    
    /**
     * Get all devices (paired and unpaired)
     * Ordered by last connected (most recent first)
     */
    @Query("SELECT * FROM device_settings ORDER BY lastConnected DESC")
    fun getAllDevices(): Flow<List<DeviceSettingEntity>>
    
    /**
     * Get device by ID (MAC address)
     */
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId")
    suspend fun getDeviceById(deviceId: String): DeviceSettingEntity?
    
    /**
     * Get device by ID as Flow
     */
    @Query("SELECT * FROM device_settings WHERE deviceId = :deviceId")
    fun getDeviceByIdFlow(deviceId: String): Flow<DeviceSettingEntity?>
    
    /**
     * Get paired devices only
     */
    @Query("SELECT * FROM device_settings WHERE isPaired = 1 ORDER BY lastConnected DESC")
    fun getPairedDevices(): Flow<List<DeviceSettingEntity>>
    
    /**
     * Get device by name
     */
    @Query("SELECT * FROM device_settings WHERE deviceName = :name LIMIT 1")
    suspend fun getDeviceByName(name: String): DeviceSettingEntity?
    
    /**
     * Search devices by name
     */
    @Query("""
        SELECT * FROM device_settings 
        WHERE deviceName LIKE '%' || :query || '%' 
        ORDER BY lastConnected DESC
    """)
    fun searchDevices(query: String): Flow<List<DeviceSettingEntity>>
    
    /**
     * Get recently connected devices
     */
    @Query("SELECT * FROM device_settings ORDER BY lastConnected DESC LIMIT :limit")
    fun getRecentDevices(limit: Int = 5): Flow<List<DeviceSettingEntity>>
    
    /**
     * Check if device exists
     */
    @Query("SELECT COUNT(*) FROM device_settings WHERE deviceId = :deviceId")
    suspend fun isDeviceExists(deviceId: String): Int
    
    /**
     * Count total devices
     */
    @Query("SELECT COUNT(*) FROM device_settings")
    suspend fun getDeviceCount(): Int
    
    /**
     * Count paired devices
     */
    @Query("SELECT COUNT(*) FROM device_settings WHERE isPaired = 1")
    suspend fun getPairedDeviceCount(): Int
    
    // ===== INSERT OPERATIONS =====
    
    /**
     * Insert device setting
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceSettingEntity)
    
    /**
     * Insert multiple devices
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceSettingEntity>)
    
    // ===== UPDATE OPERATIONS =====
    
    /**
     * Update device setting
     */
    @Update
    suspend fun updateDevice(device: DeviceSettingEntity)
    
    /**
     * Update last connected timestamp
     */
    @Query("UPDATE device_settings SET lastConnected = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateLastConnected(deviceId: String, timestamp: Long)
    
    /**
     * Update current image
     */
    @Query("UPDATE device_settings SET currentImage = :imageName WHERE deviceId = :deviceId")
    suspend fun updateCurrentImage(deviceId: String, imageName: String?)
    
    /**
     * Update current text
     */
    @Query("UPDATE device_settings SET currentText = :text WHERE deviceId = :deviceId")
    suspend fun updateCurrentText(deviceId: String, text: String?)
    
    /**
     * Update device name
     */
    @Query("UPDATE device_settings SET deviceName = :name WHERE deviceId = :deviceId")
    suspend fun updateDeviceName(deviceId: String, name: String)
    
    /**
     * Update paired status
     */
    @Query("UPDATE device_settings SET isPaired = :isPaired WHERE deviceId = :deviceId")
    suspend fun updatePairedStatus(deviceId: String, isPaired: Boolean)
    
    /**
     * Unpair all devices
     */
    @Query("UPDATE device_settings SET isPaired = 0")
    suspend fun unpairAllDevices()
    
    /**
     * Pair device (unpair others first)
     */
    @Transaction
    suspend fun pairDevice(deviceId: String) {
        unpairAllDevices()
        updatePairedStatus(deviceId, true)
        updateLastConnected(deviceId, System.currentTimeMillis())
    }
    
    /**
     * Clear device display (image and text)
     */
    @Query("UPDATE device_settings SET currentImage = NULL, currentText = NULL WHERE deviceId = :deviceId")
    suspend fun clearDeviceDisplay(deviceId: String)
    
    // ===== DELETE OPERATIONS =====
    
    /**
     * Delete device setting
     */
    @Delete
    suspend fun deleteDevice(device: DeviceSettingEntity)
    
    /**
     * Delete device by ID
     */
    @Query("DELETE FROM device_settings WHERE deviceId = :deviceId")
    suspend fun deleteDeviceById(deviceId: String)
    
    /**
     * Delete unpaired devices
     */
    @Query("DELETE FROM device_settings WHERE isPaired = 0")
    suspend fun deleteUnpairedDevices()
    
    /**
     * Delete old devices (not connected since timestamp)
     */
    @Query("DELETE FROM device_settings WHERE lastConnected < :timestamp AND isPaired = 0")
    suspend fun deleteOldDevices(timestamp: Long)
    
    /**
     * Delete all devices
     */
    @Query("DELETE FROM device_settings")
    suspend fun deleteAllDevices()
}
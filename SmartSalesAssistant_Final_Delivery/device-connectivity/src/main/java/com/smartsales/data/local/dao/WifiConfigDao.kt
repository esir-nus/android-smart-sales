package com.smartsales.data.local.dao

import androidx.room.*
import com.smartsales.data.local.entity.WifiConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiConfigDao {
    // ===== QUERY OPERATIONS =====

    /**
     * Get all WiFi configurations
     * Ordered by last used (most recent first)
     */
    @Query("SELECT * FROM wifi_configs ORDER BY lastUsed DESC")
    fun getAllWifiConfigs(): Flow<List<WifiConfigEntity>>

    /**
     * Get WiFi config by ID
     */
    @Query("SELECT * FROM wifi_configs WHERE id = :id")
    suspend fun getWifiConfigById(id: Long): WifiConfigEntity?

    /**
     * Get default WiFi configuration
     */
    @Query("SELECT * FROM wifi_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultWifiConfig(): WifiConfigEntity?

    /**
     * Get default WiFi config as Flow
     */
    @Query("SELECT * FROM wifi_configs WHERE isDefault = 1 LIMIT 1")
    fun getDefaultWifiConfigFlow(): Flow<WifiConfigEntity?>

    /**
     * Get WiFi config by SSID
     */
    @Query("SELECT * FROM wifi_configs WHERE ssid = :ssid LIMIT 1")
    suspend fun getWifiConfigBySsid(ssid: String): WifiConfigEntity?

    /**
     * Search WiFi configs by SSID
     */
    @Query(
        """
        SELECT * FROM wifi_configs 
        WHERE ssid LIKE '%' || :query || '%' 
        ORDER BY lastUsed DESC
    """,
    )
    fun searchWifiConfigs(query: String): Flow<List<WifiConfigEntity>>

    /**
     * Get recently used configs
     */
    @Query("SELECT * FROM wifi_configs ORDER BY lastUsed DESC LIMIT :limit")
    fun getRecentWifiConfigs(limit: Int = 5): Flow<List<WifiConfigEntity>>

    /**
     * Check if SSID exists
     */
    @Query("SELECT COUNT(*) FROM wifi_configs WHERE ssid = :ssid")
    suspend fun isSsidExists(ssid: String): Int

    /**
     * Count total WiFi configs
     */
    @Query("SELECT COUNT(*) FROM wifi_configs")
    suspend fun getWifiConfigCount(): Int

    // ===== INSERT OPERATIONS =====

    /**
     * Insert WiFi configuration
     * Returns the new config ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiConfig(config: WifiConfigEntity): Long

    /**
     * Insert multiple WiFi configs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWifiConfigs(configs: List<WifiConfigEntity>): List<Long>

    // ===== UPDATE OPERATIONS =====

    /**
     * Update WiFi configuration
     */
    @Update
    suspend fun updateWifiConfig(config: WifiConfigEntity)

    /**
     * Update last used timestamp
     */
    @Query("UPDATE wifi_configs SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(
        id: Long,
        timestamp: Long = System.currentTimeMillis(),
    )

    /**
     * Update password
     */
    @Query("UPDATE wifi_configs SET password = :password WHERE id = :id")
    suspend fun updatePassword(
        id: Long,
        password: String,
    )

    /**
     * Clear all default flags
     */
    @Query("UPDATE wifi_configs SET isDefault = 0")
    suspend fun clearAllDefaults()

    /**
     * Set as default
     */
    @Query("UPDATE wifi_configs SET isDefault = 1 WHERE id = :id")
    suspend fun setAsDefault(id: Long)

    /**
     * Set WiFi config as default (transaction)
     */
    @Transaction
    suspend fun setWifiConfigAsDefault(id: Long) {
        clearAllDefaults()
        setAsDefault(id)
        updateLastUsed(id)
    }

    // ===== DELETE OPERATIONS =====

    /**
     * Delete WiFi configuration
     */
    @Delete
    suspend fun deleteWifiConfig(config: WifiConfigEntity)

    /**
     * Delete WiFi config by ID
     */
    @Query("DELETE FROM wifi_configs WHERE id = :id")
    suspend fun deleteWifiConfigById(id: Long)

    /**
     * Delete WiFi config by SSID
     */
    @Query("DELETE FROM wifi_configs WHERE ssid = :ssid")
    suspend fun deleteWifiConfigBySsid(ssid: String)

    /**
     * Delete old configs (not used since timestamp)
     */
    @Query("DELETE FROM wifi_configs WHERE lastUsed < :timestamp AND isDefault = 0")
    suspend fun deleteOldConfigs(timestamp: Long)

    /**
     * Delete all WiFi configs
     */
    @Query("DELETE FROM wifi_configs")
    suspend fun deleteAllWifiConfigs()
}

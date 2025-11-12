package com.smartsales.data.local.repository

import android.content.Context
import androidx.core.content.edit
import com.smartsales.data.local.dao.DeviceSettingDao
import com.smartsales.data.local.dao.WifiConfigDao
import com.smartsales.data.local.entity.DeviceSettingEntity
import com.smartsales.data.local.entity.WifiConfigEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository
    @Inject
    constructor(
        private val deviceSettingDao: DeviceSettingDao,
        private val wifiConfigDao: WifiConfigDao,
        @ApplicationContext context: Context,
    ) {
        private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val syncedFileIds = MutableStateFlow(loadSyncedFileIds())

        // ===== DEVICE OPERATIONS =====

        /**
         * Get currently paired device
         */
        suspend fun getCurrentDevice(): DeviceSettingEntity? {
            return deviceSettingDao.getCurrentDevice()
        }

        /**
         * Get all devices (paired and unpaired)
         */
        fun getAllDevices(): Flow<List<DeviceSettingEntity>> {
            return deviceSettingDao.getAllDevices()
        }

        /**
         * Save or update device settings
         */
        suspend fun saveDevice(device: DeviceSettingEntity): Result<Unit> {
            return try {
                deviceSettingDao.insertDevice(device)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update device settings
         */
        suspend fun updateDevice(device: DeviceSettingEntity): Result<Unit> {
            return try {
                deviceSettingDao.updateDevice(device)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update last connected timestamp for a device
         */
        suspend fun updateLastConnected(deviceId: String): Result<Unit> {
            return try {
                deviceSettingDao.updateLastConnected(
                    deviceId,
                    System.currentTimeMillis(),
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update current image displayed on device
         */
        suspend fun updateCurrentImage(
            deviceId: String,
            imageName: String?,
        ): Result<Unit> {
            return try {
                deviceSettingDao.updateCurrentImage(deviceId, imageName)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update current text displayed on device
         */
        suspend fun updateCurrentText(
            deviceId: String,
            text: String?,
        ): Result<Unit> {
            return try {
                deviceSettingDao.updateCurrentText(deviceId, text)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        // ===== WIFI CONFIGURATION OPERATIONS =====

        /**
         * Get all saved WiFi configurations
         */
        fun getAllWifiConfigs(): Flow<List<WifiConfigEntity>> {
            return wifiConfigDao.getAllWifiConfigs()
        }

        /**
         * Get default WiFi configuration
         */
        suspend fun getDefaultWifiConfig(): WifiConfigEntity? {
            return wifiConfigDao.getDefaultWifiConfig()
        }

        /**
         * Save new WiFi configuration
         * Returns the new config ID
         */
        suspend fun saveWifiConfig(
            ssid: String,
            password: String,
            setAsDefault: Boolean = false,
        ): Result<Long> {
            return try {
                val config =
                    WifiConfigEntity.create(
                        ssid = ssid,
                        password = password,
                        isDefault = setAsDefault,
                    )

                // If setting as default, clear other defaults first
                if (setAsDefault) {
                    wifiConfigDao.clearAllDefaults()
                }

                val id = wifiConfigDao.insertWifiConfig(config)
                Result.success(id)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update WiFi configuration
         */
        suspend fun updateWifiConfig(config: WifiConfigEntity): Result<Unit> {
            return try {
                wifiConfigDao.updateWifiConfig(config)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Set a WiFi configuration as default
         */
        suspend fun setDefaultWifiConfig(id: Long): Result<Unit> {
            return try {
                wifiConfigDao.clearAllDefaults()
                wifiConfigDao.setAsDefault(id)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Delete WiFi configuration
         */
        suspend fun deleteWifiConfig(config: WifiConfigEntity): Result<Unit> {
            return try {
                wifiConfigDao.deleteWifiConfig(config)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        /**
         * Update last used timestamp for a WiFi config
         */
        suspend fun markWifiConfigAsUsed(id: Long): Result<Unit> {
            return try {
                val config =
                    wifiConfigDao.getWifiConfigById(id)
                        ?: return Result.failure(IllegalArgumentException("WiFi config not found: $id"))

                wifiConfigDao.updateWifiConfig(
                    config.copy(lastUsed = System.currentTimeMillis()),
                )
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun getSyncedFileIds(): List<String> {
            return syncedFileIds.value.toList()
        }

        suspend fun markFileSynced(fileId: String) {
            syncedFileIds.update { current ->
                if (current.contains(fileId)) {
                    current
                } else {
                    val updated = current + fileId
                    persistSyncedIds(updated)
                    updated
                }
            }
        }

        private fun loadSyncedFileIds(): Set<String> {
            return preferences.getStringSet(KEY_SYNCED_FILES, emptySet())?.toSet().orEmpty()
        }

        private fun persistSyncedIds(ids: Set<String>) {
            preferences.edit {
                putStringSet(KEY_SYNCED_FILES, ids.toMutableSet())
            }
        }

        companion object {
            private const val PREFS_NAME = "smart_sales_device_repo"
            private const val KEY_SYNCED_FILES = "synced_files"
        }
    }

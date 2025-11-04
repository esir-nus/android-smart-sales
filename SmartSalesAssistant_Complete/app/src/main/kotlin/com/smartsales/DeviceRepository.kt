package com.smartsales.data.repository

import com.smartsales.data.local.dao.DeviceSettingDao
import com.smartsales.data.local.dao.WifiConfigDao
import com.smartsales.data.local.entity.DeviceSettingEntity
import com.smartsales.data.local.entity.WifiConfigEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceSettingDao: DeviceSettingDao,
    private val wifiConfigDao: WifiConfigDao
) {
    
    val allWifiConfigs: Flow<List<WifiConfigEntity>> = 
        wifiConfigDao.getAllWifiConfigs()
    
    val allDevices: Flow<List<DeviceSettingEntity>> = 
        deviceSettingDao.getAllDevices()
    
    /**
     * Get currently paired device
     */
    suspend fun getCurrentDevice(): DeviceSettingEntity? {
        return deviceSettingDao.getCurrentDevice()
    }
    
    /**
     * Save or update a device
     */
    suspend fun saveDevice(
        deviceId: String,
        deviceName: String
    ) {
        deviceSettingDao.insertDevice(
            DeviceSettingEntity(
                deviceId = deviceId,
                deviceName = deviceName,
                lastConnected = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Update device last connected timestamp
     */
    suspend fun updateDeviceConnection(deviceId: String) {
        deviceSettingDao.updateLastConnected(
            deviceId, 
            System.currentTimeMillis()
        )
    }
    
    /**
     * Update current display image on device
     */
    suspend fun updateDeviceImage(deviceId: String, imageName: String?) {
        deviceSettingDao.updateCurrentImage(deviceId, imageName)
    }
    
    /**
     * Update current display text on device
     */
    suspend fun updateDeviceText(deviceId: String, text: String?) {
        deviceSettingDao.updateCurrentText(deviceId, text)
    }
    
    /**
     * Save WiFi configuration
     */
    suspend fun saveWifiConfig(
        ssid: String, 
        password: String, 
        isDefault: Boolean = false
    ): Long {
        if (isDefault) {
            wifiConfigDao.clearAllDefaults()
        }
        return wifiConfigDao.insertWifiConfig(
            WifiConfigEntity(
                ssid = ssid,
                password = password,
                isDefault = isDefault,
                lastUsed = System.currentTimeMillis(),
                addedAt = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Get default WiFi configuration
     */
    suspend fun getDefaultWifi(): WifiConfigEntity? {
        return wifiConfigDao.getDefaultWifiConfig()
    }
    
    /**
     * Update WiFi last used timestamp
     */
    suspend fun updateWifiLastUsed(wifiId: Long) {
        wifiConfigDao.getDefaultWifiConfig()?.let { config ->
            wifiConfigDao.updateWifiConfig(
                config.copy(lastUsed = System.currentTimeMillis())
            )
        }
    }
    
    /**
     * Delete WiFi configuration
     */
    suspend fun deleteWifiConfig(wifiId: Long) {
        // Implementation would need to get entity first
        // Simplified for now
    }
    
    /**
     * Set a WiFi config as default
     */
    suspend fun setDefaultWifi(wifiId: Long) {
        wifiConfigDao.clearAllDefaults()
        wifiConfigDao.setAsDefault(wifiId)
    }
}

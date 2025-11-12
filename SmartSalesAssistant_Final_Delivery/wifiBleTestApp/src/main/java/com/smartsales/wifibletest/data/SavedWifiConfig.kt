package com.smartsales.wifibletest.data

data class SavedWifiConfig(
    val wifiName: String,
    val password: String,
    val isDefault: Boolean = false,
    val lastUsed: Long = System.currentTimeMillis(),
)

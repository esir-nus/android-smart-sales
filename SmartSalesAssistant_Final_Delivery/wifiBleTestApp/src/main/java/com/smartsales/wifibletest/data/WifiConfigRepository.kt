package com.smartsales.wifibletest.data

import android.content.Context
import androidx.core.content.edit
import com.smartsales.data.network.ConnectivityApiConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiConfigRepository
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        private val _configs = MutableStateFlow(loadConfigs())
        val configs: StateFlow<List<SavedWifiConfig>> = _configs.asStateFlow()
        private val _deviceIp = MutableStateFlow(loadDeviceIp())
        val deviceIp: StateFlow<String> = _deviceIp.asStateFlow()
        private val _devicePort = MutableStateFlow(loadDevicePort())
        val devicePort: StateFlow<Int> = _devicePort.asStateFlow()

        private fun loadConfigs(): List<SavedWifiConfig> {
            val json = prefs.getString(KEY_CONFIGS, null) ?: return emptyList()
            return runCatching {
                val array = JSONArray(json)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val storedName =
                            obj.optString("wifiName")
                                .ifBlank { obj.optString("ssid") }
                        add(
                            SavedWifiConfig(
                                wifiName = storedName,
                                password = obj.optString("password"),
                                isDefault = obj.optBoolean("isDefault"),
                                lastUsed = obj.optLong("lastUsed"),
                            ),
                        )
                    }
                }
            }.getOrDefault(emptyList())
        }

        private fun persistConfigs(configs: List<SavedWifiConfig>) {
            val array = JSONArray()
            configs.forEach { config ->
                array.put(
                    JSONObject().apply {
                        put("wifiName", config.wifiName)
                        put("ssid", config.wifiName)
                        put("password", config.password)
                        put("isDefault", config.isDefault)
                        put("lastUsed", config.lastUsed)
                    },
                )
            }

            prefs.edit {
                putString(KEY_CONFIGS, array.toString())
            }
        }

        suspend fun saveWifiConfig(
            wifiName: String,
            password: String,
            setAsDefault: Boolean,
        ) {
            withContext(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                val current = _configs.value.toMutableList()
                val existingIndex = current.indexOfFirst { it.wifiName == wifiName }

                if (existingIndex >= 0) {
                    val existing = current[existingIndex]
                    current[existingIndex] =
                        existing.copy(
                            password = if (password.isNotEmpty()) password else existing.password,
                            isDefault = if (setAsDefault) true else existing.isDefault,
                            lastUsed = now,
                        )
                } else {
                    current.add(
                        SavedWifiConfig(
                            wifiName = wifiName,
                            password = password,
                            isDefault = setAsDefault,
                            lastUsed = now,
                        ),
                    )
                }

                val normalized =
                    if (setAsDefault) {
                        current.map { config ->
                            if (config.wifiName == wifiName) {
                                config.copy(isDefault = true, lastUsed = now)
                            } else {
                                config.copy(isDefault = false)
                            }
                        }
                    } else {
                        current
                    }

                val sorted = normalized.sortedByDescending { it.lastUsed }
                persistConfigs(sorted)
                _configs.value = sorted
            }
        }

        private fun loadDeviceIp(): String {
            return prefs.getString(KEY_DEVICE_IP, "").orEmpty()
        }

        private fun loadDevicePort(): Int {
            return prefs.getInt(KEY_DEVICE_PORT, ConnectivityApiConfig.DEFAULT_PORT)
        }

        suspend fun updateDeviceEndpoint(
            ip: String,
            port: Int,
        ) {
            withContext(Dispatchers.IO) {
                prefs.edit {
                    putString(KEY_DEVICE_IP, ip)
                    putInt(KEY_DEVICE_PORT, port)
                }
                _deviceIp.value = ip
                _devicePort.value = port
            }
        }

        suspend fun updateDeviceIp(ip: String) {
            updateDeviceEndpoint(ip, _devicePort.value)
        }

        suspend fun updateDevicePort(port: Int) {
            updateDeviceEndpoint(_deviceIp.value, port)
        }

        companion object {
            private const val PREFS_NAME = "wifi_configs"
            private const val KEY_CONFIGS = "entries"
            private const val KEY_DEVICE_IP = "device_ip"
            private const val KEY_DEVICE_PORT = "device_port"
        }
    }

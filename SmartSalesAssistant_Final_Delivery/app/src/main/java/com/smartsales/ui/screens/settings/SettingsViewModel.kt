package com.smartsales.ui.screens.settings

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings UI State
 */
data class SettingsUiState(
    val apiKey: String = "",
    val hasApiKey: Boolean = false,
    val selectedModel: String = "qwen-turbo",
    val autoSync: Boolean = false,
    val deleteAfterSync: Boolean = false,
    val downloadPath: String = "/storage/emulated/0/Download/SmartSales",
    val appVersion: String = "1.0.0",
)

/**
 * Settings ViewModel
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : ViewModel() {
        private val preferences =
            context.getSharedPreferences(
                "smart_sales_settings",
                Context.MODE_PRIVATE,
            )

        private val _uiState = MutableStateFlow(SettingsUiState())
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            loadSettings()
        }

        /**
         * Load settings from SharedPreferences
         */
        private fun loadSettings() {
            val apiKey = preferences.getString("api_key", "") ?: ""
            val selectedModel = preferences.getString("selected_model", "qwen-turbo") ?: "qwen-turbo"
            val autoSync = preferences.getBoolean("auto_sync", false)
            val deleteAfterSync = preferences.getBoolean("delete_after_sync", false)
            val downloadPath =
                preferences.getString("download_path", "/storage/emulated/0/Download/SmartSales")
                    ?: "/storage/emulated/0/Download/SmartSales"

            // Get app version
            val appVersion =
                try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    packageInfo.versionName ?: "1.0.0"
                } catch (e: Exception) {
                    "1.0.0"
                }

            _uiState.update {
                it.copy(
                    apiKey = apiKey,
                    hasApiKey = apiKey.isNotBlank(),
                    selectedModel = selectedModel,
                    autoSync = autoSync,
                    deleteAfterSync = deleteAfterSync,
                    downloadPath = downloadPath,
                    appVersion = appVersion,
                )
            }
        }

        /**
         * Save API key
         */
        fun saveApiKey(apiKey: String) {
            viewModelScope.launch {
                preferences.edit {
                    putString("api_key", apiKey)
                }

                _uiState.update {
                    it.copy(
                        apiKey = apiKey,
                        hasApiKey = apiKey.isNotBlank(),
                    )
                }
            }
        }

        /**
         * Set auto sync
         */
        fun setAutoSync(enabled: Boolean) {
            viewModelScope.launch {
                preferences.edit {
                    putBoolean("auto_sync", enabled)
                }

                _uiState.update {
                    it.copy(autoSync = enabled)
                }
            }
        }

        /**
         * Set delete after sync
         */
        fun setDeleteAfterSync(enabled: Boolean) {
            viewModelScope.launch {
                preferences.edit {
                    putBoolean("delete_after_sync", enabled)
                }

                _uiState.update {
                    it.copy(deleteAfterSync = enabled)
                }
            }
        }

        /**
         * Clear cache
         */
        fun clearCache() {
            viewModelScope.launch {
                try {
                    context.cacheDir.deleteRecursively()

                    // Show success message (you can add a Snackbar state)
                } catch (e: Exception) {
                    // Show error message
                }
            }
        }
    }

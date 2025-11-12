package com.smartsales.ui.screens.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsales.data.local.repository.DeviceRepository
import com.smartsales.data.network.api.GadgetApi
import com.smartsales.data.network.api.GadgetApiHelper
import com.smartsales.data.network.model.GadgetFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * File Sync UI State
 */
data class FileSyncUiState(
    val files: List<GadgetFile> = emptyList(),
    val syncedFileIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val isConnected: Boolean = false,
    val currentSyncFile: String? = null,
    val syncProgress: Float = 0f,
    val filesSynced: Int = 0,
    val totalFilesToSync: Int = 0,
    val error: String? = null,
)

/**
 * File Sync ViewModel
 */
@HiltViewModel
class FileSyncViewModel
    @Inject
    constructor(
        private val gadgetApi: GadgetApi,
        private val deviceRepository: DeviceRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(FileSyncUiState())
        val uiState: StateFlow<FileSyncUiState> = _uiState.asStateFlow()

        private val downloadDirectory = "/storage/emulated/0/Download/SmartSales"

        init {
            checkConnection()
        }

        /**
         * Check device connection
         */
        fun checkConnection() {
            viewModelScope.launch {
                val isReachable =
                    GadgetApiHelper.isDeviceReachable(
                        api = gadgetApi,
                        timeoutMs = 5000,
                    )

                _uiState.update {
                    it.copy(isConnected = isReachable)
                }

                if (isReachable) {
                    refreshFileList()
                }
            }
        }

        /**
         * Refresh file list from device
         */
        fun refreshFileList() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                try {
                    val response = gadgetApi.getFileList()

                    if (response.isSuccessful) {
                        val fileList = response.body()

                        if (fileList != null) {
                            // Get synced file IDs from local database
                            val syncedIds = deviceRepository.getSyncedFileIds()

                            _uiState.update {
                                it.copy(
                                    files = fileList.files,
                                    syncedFileIds = syncedIds.toSet(),
                                    isLoading = false,
                                    isConnected = true,
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "获取文件列表失败",
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "设备响应失败: ${response.code()}",
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isConnected = false,
                            error = e.message ?: "连接失败",
                        )
                    }
                }
            }
        }

        /**
         * Sync single file
         */
        fun syncFile(file: GadgetFile) {
            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isSyncing = true,
                        currentSyncFile = file.name,
                        syncProgress = 0f,
                        filesSynced = 0,
                        totalFilesToSync = 1,
                    )
                }

                try {
                    val localPath = "$downloadDirectory/${file.name}"

                    val result =
                        GadgetApiHelper.downloadFileToLocal(
                            api = gadgetApi,
                            fileId = file.id,
                            localPath = localPath,
                            progressCallback = { downloaded, total ->
                                val progress =
                                    if (total > 0) {
                                        (downloaded.toFloat() / total)
                                    } else {
                                        0f
                                    }

                                _uiState.update {
                                    it.copy(syncProgress = progress)
                                }
                            },
                        )

                    result.onSuccess {
                        // Mark as synced in database
                        deviceRepository.markFileSynced(file.id)

                        // Mark on device
                        gadgetApi.markFileSynced(file.id)

                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncedFileIds = it.syncedFileIds + file.id,
                                filesSynced = 1,
                            )
                        }
                    }

                    result.onFailure { exception ->
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                error = "下载失败: ${exception.message}",
                            )
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            error = e.message ?: "下载失败",
                        )
                    }
                }
            }
        }

        /**
         * Sync all files
         */
        fun syncAllFiles() {
            viewModelScope.launch {
                val unsyncedFiles =
                    _uiState.value.files.filter { file ->
                        !_uiState.value.syncedFileIds.contains(file.id)
                    }

                if (unsyncedFiles.isEmpty()) return@launch

                _uiState.update {
                    it.copy(
                        isSyncing = true,
                        totalFilesToSync = unsyncedFiles.size,
                        filesSynced = 0,
                    )
                }

                try {
                    unsyncedFiles.forEachIndexed { index, file ->
                        _uiState.update {
                            it.copy(
                                currentSyncFile = file.name,
                                syncProgress = 0f,
                            )
                        }

                        val localPath = "$downloadDirectory/${file.name}"

                        val result =
                            GadgetApiHelper.downloadFileToLocal(
                                api = gadgetApi,
                                fileId = file.id,
                                localPath = localPath,
                                progressCallback = { downloaded, total ->
                                    val progress =
                                        if (total > 0) {
                                            (downloaded.toFloat() / total)
                                        } else {
                                            0f
                                        }

                                    _uiState.update {
                                        it.copy(syncProgress = progress)
                                    }
                                },
                            )

                        if (result.isSuccess) {
                            // Mark as synced
                            deviceRepository.markFileSynced(file.id)
                            gadgetApi.markFileSynced(file.id)

                            _uiState.update {
                                it.copy(
                                    syncedFileIds = it.syncedFileIds + file.id,
                                    filesSynced = index + 1,
                                )
                            }
                        }
                    }

                    _uiState.update {
                        it.copy(isSyncing = false)
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            error = e.message ?: "批量同步失败",
                        )
                    }
                }
            }
        }

        /**
         * Delete file from device
         */
        fun deleteFile(file: GadgetFile) {
            viewModelScope.launch {
                try {
                    val response = gadgetApi.deleteFile(file.id)

                    if (response.isSuccessful) {
                        // Remove from local list
                        _uiState.update {
                            it.copy(
                                files = it.files.filter { f -> f.id != file.id },
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(error = "删除失败: ${response.code()}")
                        }
                    }
                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(error = e.message ?: "删除失败")
                    }
                }
            }
        }
    }

package com.smartsales.data.network.api

import com.smartsales.data.network.ApiConfig
import com.smartsales.data.network.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Gadget HTTP API Interface
 * 
 * Communication with hardware device's HTTP server
 * 
 * The device runs a local HTTP server for:
 * - File listing and download
 * - Device status queries
 * - WiFi status
 * - File management
 */
interface GadgetApi {
    
    /**
     * Get File List
     * 
     * Retrieve list of all recorded files on device
     * 
     * Example:
     * ```
     * val response = api.getFileList()
     * val files = response.files
     * files.forEach { file ->
     *     println("${file.name}: ${file.getFormattedSize()}")
     * }
     * ```
     */
    @GET(ApiConfig.Gadget.FILE_LIST)
    suspend fun getFileList(): Response<FileListResponse>
    
    /**
     * Get File List with Filters
     * 
     * Filter files by type, date range, etc.
     */
    @GET(ApiConfig.Gadget.FILE_LIST)
    suspend fun getFileListFiltered(
        @Query("type") type: String? = null, // "audio", "image"
        @Query("from") fromTimestamp: Long? = null,
        @Query("to") toTimestamp: Long? = null,
        @Query("synced") synced: Boolean? = null
    ): Response<FileListResponse>
    
    /**
     * Download File
     * 
     * Download a specific file from device
     * 
     * Example:
     * ```
     * val response = api.downloadFile(fileId = "audio_123")
     * val fileBytes = response.body()?.bytes()
     * ```
     */
    @GET(ApiConfig.Gadget.FILE_DOWNLOAD)
    @Streaming
    suspend fun downloadFile(
        @Query("file_id") fileId: String
    ): Response<ResponseBody>
    
    /**
     * Download File by Path
     */
    @GET(ApiConfig.Gadget.FILE_DOWNLOAD)
    @Streaming
    suspend fun downloadFileByPath(
        @Query("path") filePath: String
    ): Response<ResponseBody>
    
    /**
     * Delete Files
     * 
     * Delete one or more files from device
     * 
     * Example:
     * ```
     * val request = FileDeleteRequest(
     *     fileIds = listOf("file1", "file2", "file3")
     * )
     * val response = api.deleteFiles(request)
     * println("Deleted: ${response.deletedCount}")
     * ```
     */
    @HTTP(method = "DELETE", path = ApiConfig.Gadget.FILE_DELETE, hasBody = true)
    suspend fun deleteFiles(
        @Body request: FileDeleteRequest
    ): Response<FileDeleteResponse>
    
    /**
     * Delete Single File
     */
    @DELETE(ApiConfig.Gadget.FILE_DELETE)
    suspend fun deleteFile(
        @Query("file_id") fileId: String
    ): Response<FileDeleteResponse>
    
    /**
     * Get Device Status
     * 
     * Get current device status (battery, storage, etc.)
     * 
     * Example:
     * ```
     * val status = api.getDeviceStatus()
     * println("Battery: ${status.batteryLevel}%")
     * println("Storage: ${status.getFormattedStorageUsed()} / ${status.getFormattedStorageTotal()}")
     * ```
     */
    @GET(ApiConfig.Gadget.DEVICE_STATUS)
    suspend fun getDeviceStatus(): Response<DeviceStatusResponse>
    
    /**
     * Get WiFi Status
     * 
     * Check WiFi connection status
     * 
     * Example:
     * ```
     * val wifi = api.getWifiStatus()
     * if (wifi.connected) {
     *     println("Connected to: ${wifi.ssid}")
     *     println("Signal: ${wifi.getSignalQuality()}")
     * }
     * ```
     */
    @GET(ApiConfig.Gadget.WIFI_STATUS)
    suspend fun getWifiStatus(): Response<WifiStatusResponse>
    
    /**
     * Ping Device
     * 
     * Simple ping to check if device is reachable
     */
    @GET("api/ping")
    suspend fun ping(): Response<Unit>
    
    /**
     * Mark File as Synced
     * 
     * Update file's sync status on device
     */
    @POST("api/files/mark-synced")
    suspend fun markFileSynced(
        @Query("file_id") fileId: String
    ): Response<Unit>
}

/**
 * Gadget API Helper
 * 
 * Utility functions for working with Gadget API
 */
object GadgetApiHelper {
    
    /**
     * Check if device is reachable
     */
    suspend fun isDeviceReachable(
        api: GadgetApi,
        timeoutMs: Long = 5000
    ): Boolean {
        return try {
            val response = kotlinx.coroutines.withTimeout(timeoutMs) {
                api.ping()
            }
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Download file to local storage
     * 
     * @param api Gadget API instance
     * @param fileId File ID on device
     * @param localPath Local file path to save
     * @param progressCallback Progress callback (bytesDownloaded, totalBytes)
     */
    suspend fun downloadFileToLocal(
        api: GadgetApi,
        fileId: String,
        localPath: String,
        progressCallback: ((Long, Long) -> Unit)? = null
    ): Result<String> {
        return try {
            val response = api.downloadFile(fileId)
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("Download failed: ${response.code()}"))
            }
            
            val body = response.body() ?: return Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()
            
            val file = java.io.File(localPath)
            file.parentFile?.mkdirs()
            
            file.outputStream().use { output ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        progressCallback?.invoke(totalBytesRead, contentLength)
                    }
                }
            }
            
            Result.success(localPath)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sync all new files from device
     * 
     * Downloads all unsynced files
     */
    suspend fun syncAllNewFiles(
        api: GadgetApi,
        localDirectory: String,
        progressCallback: ((Int, Int) -> Unit)? = null
    ): Result<List<String>> {
        return try {
            // Get file list
            val listResponse = api.getFileListFiltered(synced = false)
            
            if (!listResponse.isSuccessful) {
                return Result.failure(Exception("Failed to get file list"))
            }
            
            val files = listResponse.body()?.files ?: emptyList()
            val downloadedPaths = mutableListOf<String>()
            
            files.forEachIndexed { index, file ->
                val localPath = "$localDirectory/${file.name}"
                
                val result = downloadFileToLocal(api, file.id, localPath)
                
                if (result.isSuccess) {
                    downloadedPaths.add(localPath)
                    
                    // Mark as synced on device
                    api.markFileSynced(file.id)
                }
                
                progressCallback?.invoke(index + 1, files.size)
            }
            
            Result.success(downloadedPaths)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get storage summary
     */
    suspend fun getStorageSummary(api: GadgetApi): StorageSummary? {
        return try {
            val statusResponse = api.getDeviceStatus()
            val fileListResponse = api.getFileList()
            
            if (statusResponse.isSuccessful && fileListResponse.isSuccessful) {
                val status = statusResponse.body()!!
                val fileList = fileListResponse.body()!!
                
                val audioFiles = fileList.files.count { it.isAudio() }
                val imageFiles = fileList.files.count { it.isImage() }
                
                StorageSummary(
                    totalSpace = status.storageTotal,
                    usedSpace = status.storageUsed,
                    freeSpace = status.storageTotal - status.storageUsed,
                    fileCount = fileList.totalCount,
                    audioFileCount = audioFiles,
                    imageFileCount = imageFiles,
                    totalFileSize = fileList.totalSize
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate estimated sync time
     */
    fun estimateSyncTime(
        fileSizeBytes: Long,
        networkSpeedBytesPerSec: Long = 1_000_000 // 1 MB/s default
    ): Long {
        return fileSizeBytes / networkSpeedBytesPerSec
    }
    
    /**
     * Format sync speed
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond < 1024 -> "${bytesPerSecond} B/s"
            bytesPerSecond < 1024 * 1024 -> "${bytesPerSecond / 1024} KB/s"
            else -> "%.2f MB/s".format(bytesPerSecond / (1024.0 * 1024.0))
        }
    }
    
    /**
     * Validate device connectivity
     */
    suspend fun validateConnectivity(api: GadgetApi): ConnectivityStatus {
        return try {
            // Check ping
            val pingSuccess = isDeviceReachable(api, timeoutMs = 3000)
            
            if (!pingSuccess) {
                return ConnectivityStatus(
                    reachable = false,
                    wifiConnected = false,
                    error = "Device not reachable"
                )
            }
            
            // Check WiFi status
            val wifiResponse = api.getWifiStatus()
            val wifiConnected = wifiResponse.isSuccessful && 
                                wifiResponse.body()?.connected == true
            
            ConnectivityStatus(
                reachable = true,
                wifiConnected = wifiConnected,
                error = null
            )
            
        } catch (e: Exception) {
            ConnectivityStatus(
                reachable = false,
                wifiConnected = false,
                error = e.message
            )
        }
    }
}

/**
 * Storage summary data class
 */
data class StorageSummary(
    val totalSpace: Long,
    val usedSpace: Long,
    val freeSpace: Long,
    val fileCount: Int,
    val audioFileCount: Int,
    val imageFileCount: Int,
    val totalFileSize: Long
) {
    fun getUsagePercent(): Int {
        return if (totalSpace > 0) {
            ((usedSpace.toFloat() / totalSpace) * 100).toInt()
        } else 0
    }
}

/**
 * Connectivity status data class
 */
data class ConnectivityStatus(
    val reachable: Boolean,
    val wifiConnected: Boolean,
    val error: String?
)
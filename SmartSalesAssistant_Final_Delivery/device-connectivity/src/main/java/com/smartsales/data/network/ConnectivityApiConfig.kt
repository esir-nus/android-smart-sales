package com.smartsales.data.network

/**
 * Connectivity API configuration shared by BLE/Wi-Fi features.
 */
object ConnectivityApiConfig {

    const val DEFAULT_SCHEME = "http"
    const val DEFAULT_PORT = 8000

    const val CONNECT_TIMEOUT_MS = 10_000L
    const val READ_TIMEOUT_MS = 30_000L
    const val WRITE_TIMEOUT_MS = 30_000L

    object Gadget {
        const val FILE_LIST = "api/files"
        const val FILE_DOWNLOAD = "api/files/download"
        const val FILE_DELETE = "api/files/delete"
        const val DEVICE_STATUS = "api/status"
        const val WIFI_STATUS = "api/wifi/status"
        const val PING = "api/ping"
        const val MARK_SYNCED = "api/files/mark-synced"
    }

    fun buildBaseUrl(ipAddress: String, port: Int = DEFAULT_PORT): String {
        return "$DEFAULT_SCHEME://$ipAddress:$port/"
    }
}

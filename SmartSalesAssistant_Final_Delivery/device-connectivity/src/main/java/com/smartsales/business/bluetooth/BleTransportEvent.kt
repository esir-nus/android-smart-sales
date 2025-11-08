package com.smartsales.business.bluetooth

data class BleTransportEvent(
    val direction: BleTransportDirection,
    val payload: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
)

enum class BleTransportDirection {
    SENT,
    RECEIVED
}

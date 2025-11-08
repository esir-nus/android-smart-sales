package com.smartsales.business.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.text.Charsets

class BleConstantsTest {

    private fun ByteArray.toWifiPayloadMap(): Map<String, String> {
        val content = String(this, Charsets.UTF_8).trim()
        if (content.isEmpty()) return emptyMap()
        val body = content.removePrefix("{").removeSuffix("}")
        if (body.isBlank()) return emptyMap()
        return body.split(",")
            .map { entry ->
                val parts = entry.split(":", limit = 2)
                val key = parts.getOrNull(0)?.trim()?.trim('"').orEmpty()
                val value = parts.getOrNull(1)?.trim()?.trim('"').orEmpty()
                key to value
            }
            .toMap()
    }

    @Test
    fun `encodeWifiConfig builds json payload with ssid and password`() {
        val payload = BleConstants.encodeWifiConfig("SmartSales", "secret1")
        val json = payload.toWifiPayloadMap()

        assertEquals("SmartSales", json["ssid"])
        assertEquals("secret1", json["password"])
    }

    @Test
    fun `encodeWifiConfig trims values beyond max protocol length`() {
        val longSsid = "S".repeat(BleConstants.MAX_SSID_LENGTH + 5)
        val longPassword = "P".repeat(BleConstants.MAX_PASSWORD_LENGTH + 10)

        val payload = BleConstants.encodeWifiConfig(longSsid, longPassword)

        val json = payload.toWifiPayloadMap()

        assertEquals(BleConstants.MAX_SSID_LENGTH, json["ssid"]?.length)
        assertEquals(BleConstants.MAX_PASSWORD_LENGTH, json["password"]?.length)
    }

    @Test
    fun `encodeDisplayText prefixes command and length`() {
        val payload = BleConstants.encodeDisplayText("Hello World")

        assertEquals(0x04, payload[0].toInt() and 0xFF)
        assertEquals(11, payload[1].toInt() and 0xFF)
        assertArrayEquals(
            "Hello World".toByteArray(),
            payload.copyOfRange(2, payload.size)
        )
    }

    @Test
    fun `encodeDisplayText trims text beyond max length`() {
        val longText = "T".repeat(BleConstants.MAX_DISPLAY_TEXT_LENGTH + 40)

        val payload = BleConstants.encodeDisplayText(longText)

        assertEquals(
            BleConstants.MAX_DISPLAY_TEXT_LENGTH,
            payload[1].toInt() and 0xFF
        )
    }

    @Test
    fun `encodeDisplayImage prefixes command and length`() {
        val payload = BleConstants.encodeDisplayImage("image.png")

        assertEquals(0x03, payload[0].toInt() and 0xFF)
        assertEquals(9, payload[1].toInt() and 0xFF)
        assertArrayEquals(
            "image.png".toByteArray(),
            payload.copyOfRange(2, payload.size)
        )
    }

    @Test
    fun `encodeDisplayImage trims filename beyond max length`() {
        val longName = "I".repeat(BleConstants.MAX_IMAGE_NAME_LENGTH + 15)

        val payload = BleConstants.encodeDisplayImage(longName)

        assertEquals(
            BleConstants.MAX_IMAGE_NAME_LENGTH,
            payload[1].toInt() and 0xFF
        )
    }

    @Test
    fun `decodeDeviceStatus returns unknown when insufficient data`() {
        val status = BleConstants.decodeDeviceStatus(byteArrayOf(0x01))

        assertEquals(0, status.batteryLevel)
        assertFalse(status.isWifiConnected)
        assertFalse(status.isRecording)
    }

    @Test
    fun `decodeDeviceStatus parses battery wifi and recording flags`() {
        val status = BleConstants.decodeDeviceStatus(
            byteArrayOf(75, 1, 0)
        )

        assertEquals(75, status.batteryLevel)
        assertTrue(status.isWifiConnected)
        assertFalse(status.isRecording)
    }

    @Test
    fun `chunkData splits bytes into requested chunk size`() {
        val data = ByteArray(45) { it.toByte() }

        val chunks = BleConstants.chunkData(data, chunkSize = 10)

        assertEquals(5, chunks.size)
        assertEquals(10, chunks[0].size)
        assertEquals(5, chunks.last().size)
    }

    @Test
    fun `calculateChecksum sums byte values`() {
        val checksum = BleConstants.calculateChecksum(byteArrayOf(0x01, 0x02, 0x03))

        assertEquals(0x06, checksum.toInt() and 0xFF)
    }

    @Test
    fun `verifyChecksum validates checksum at end of payload`() {
        val data = byteArrayOf(0x10, 0x20, 0x30)
        val checksum = BleConstants.calculateChecksum(data)
        val payload = data + checksum

        assertTrue(BleConstants.verifyChecksum(payload))
    }

    @Test
    fun `verifyChecksum rejects invalid checksum`() {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0xFF.toByte())

        assertFalse(BleConstants.verifyChecksum(payload))
    }

    @Test
    fun `DeviceStatus flags report low and critical battery states`() {
        val status = DeviceStatus(batteryLevel = 15, isWifiConnected = false, isRecording = false)
        val critical = DeviceStatus(batteryLevel = 5, isWifiConnected = false, isRecording = false)

        assertTrue(status.isBatteryLow)
        assertFalse(status.isBatteryCritical)
        assertTrue(critical.isBatteryCritical)
    }

    @Test
    fun `DeviceStatus summary reflects battery wifi and recording status`() {
        val status = DeviceStatus(
            batteryLevel = 82,
            isWifiConnected = true,
            isRecording = true,
            timestamp = 123L
        )

        val summary = status.getSummary()

        assertTrue(summary.contains("Battery: 82% (Full)"))
        assertTrue(summary.contains("WiFi: Connected"))
        assertTrue(summary.contains("Recording: Active"))
    }
}

package com.smartsales.business.bluetooth

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BleCommandTest {

    @Test
    fun `toByteArray emits enum value`() {
        val command = BleCommand.START_RECORDING

        val result = command.toByteArray()

        assertArrayEquals(byteArrayOf(0x01), result)
    }

    @Test
    fun `toByteArrayWithParam prefixes command`() {
        val param: Byte = 0x42

        val result = BleCommand.SHOW_TEXT.toByteArrayWithParam(param)

        assertArrayEquals(byteArrayOf(0x04, 0x42), result)
    }

    @Test
    fun `toByteArrayWithParams handles multiple values`() {
        val params = byteArrayOf(0x10, 0x11, 0x12)

        val result = BleCommand.SHOW_IMAGE.toByteArrayWithParams(*params)

        assertArrayEquals(byteArrayOf(0x03, 0x10, 0x11, 0x12), result)
    }

    @Test
    fun `fromByte resolves matching command`() {
        val command = BleCommand.fromByte(0x05)

        assertNotNull(command)
        assertEquals(BleCommand.CLEAR_DISPLAY, command)
    }

    @Test
    fun `fromByte returns null for unknown value`() {
        val command = BleCommand.fromByte(0x7F)

        assertNull(command)
    }

    @Test
    fun `isValidCommand matches defined values`() {
        assertTrue(BleCommand.isValidCommand(0x09))
        assertFalse(BleCommand.isValidCommand(0x00))
    }

    @Test
    fun `requiresParameters true only for display commands`() {
        assertTrue(BleCommand.SHOW_TEXT.requiresParameters())
        assertTrue(BleCommand.SHOW_IMAGE.requiresParameters())
        assertFalse(BleCommand.START_RECORDING.requiresParameters())
    }

    @Test
    fun `isDestructive true for device reset commands`() {
        assertTrue(BleCommand.FACTORY_RESET.isDestructive())
        assertTrue(BleCommand.RESTART_DEVICE.isDestructive())
        assertFalse(BleCommand.CLEAR_DISPLAY.isDestructive())
    }

    @Test
    fun `getEstimatedExecutionTime returns documented durations`() {
        assertEquals(500L, BleCommand.START_RECORDING.getEstimatedExecutionTime())
        assertEquals(5000L, BleCommand.START_FILE_SYNC.getEstimatedExecutionTime())
        assertEquals(1000L, BleCommand.REQUEST_STATUS.getEstimatedExecutionTime())
    }
}

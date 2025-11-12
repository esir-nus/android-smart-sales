package com.smartsales.ui.screens.device

import android.bluetooth.BluetoothDevice
import com.smartsales.business.bluetooth.BleConnectionState
import com.smartsales.business.bluetooth.BleManager
import com.smartsales.data.local.repository.DeviceRepository
import com.smartsales.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DevicePairingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val bleManager: BleManager = mock()
    private val deviceRepository: DeviceRepository = mock()
    private val connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    private val discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())

    private fun createViewModel(): DevicePairingViewModel {
        whenever(bleManager.connectionState).thenReturn(connectionState)
        whenever(bleManager.discoveredDevices).thenReturn(discoveredDevices)
        runBlocking {
            whenever(bleManager.sendWifiConfig(any(), any())).thenReturn(Result.success(Unit))
        }
        whenever(bleManager.hasConnectPermission()).thenReturn(true)
        return DevicePairingViewModel(bleManager, deviceRepository)
    }

    @Test
    fun startScan_withoutPermission_setsErrorAndSkipsScan() =
        runTest {
            whenever(bleManager.hasScanPermission()).thenReturn(false)
            whenever(bleManager.isBluetoothEnabled()).thenReturn(true)

            val viewModel = createViewModel()

            viewModel.startScan()

            assertEquals("请先授予蓝牙扫描权限", viewModel.uiState.value.error)
            verify(bleManager, never()).startScan()
        }

    @Test
    fun startScan_withPermission_invokesBleManager() =
        runTest {
            whenever(bleManager.hasScanPermission()).thenReturn(true)
            whenever(bleManager.isBluetoothEnabled()).thenReturn(true)

            val viewModel = createViewModel()

            viewModel.startScan()

            verify(bleManager).startScan()
            assertEquals(null, viewModel.uiState.value.error)
        }
}

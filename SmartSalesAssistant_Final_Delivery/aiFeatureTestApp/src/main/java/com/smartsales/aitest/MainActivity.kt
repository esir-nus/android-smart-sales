package com.smartsales.aitest

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.smartsales.aitest.ui.AiTestScreen
import com.smartsales.aitest.ui.AiTestViewModel
import com.smartsales.aitest.ui.theme.AiFeatureTestAppTheme
import com.smartsales.aitest.util.AudioFilePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val aiTestViewModel: AiTestViewModel by viewModels()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var hasBlePermissions by mutableStateOf(false)

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            lifecycleScope.launch {
                val audioFile = AudioFilePicker.getFileFromUri(this@MainActivity, uri)
                if (audioFile != null) {
                    val validationResult = AudioFilePicker.validateAudioFile(audioFile)
                    if (validationResult.isSuccess) {
                        aiTestViewModel.selectLocalAudioFile(audioFile)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                hasBlePermissions = result.values.all { it }
            }
        hasBlePermissions = areBlePermissionsGranted()

        setContent {
            AiFeatureTestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AiTestScreen(
                        onSelectLocalFile = { filePickerLauncher.launch("audio/*") },
                        hasBlePermissions = hasBlePermissions,
                        onRequestBlePermissions = { requestBlePermissions() },
                    )
                }
            }
        }
    }

    private fun requestBlePermissions() {
        if (hasBlePermissions) return
        permissionLauncher.launch(requiredBlePermissions())
    }

    private fun areBlePermissionsGranted(): Boolean =
        requiredBlePermissions().all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredBlePermissions(): Array<String> {
        val permissions =
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_CONNECT
        } else {
            permissions += Manifest.permission.BLUETOOTH
            permissions += Manifest.permission.BLUETOOTH_ADMIN
        }
        return permissions.toTypedArray()
    }
}

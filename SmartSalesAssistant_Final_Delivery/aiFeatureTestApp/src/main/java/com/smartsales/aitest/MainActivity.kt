package com.smartsales.aitest

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.smartsales.aitest.ui.AiTestScreen
import com.smartsales.aitest.ui.AiTestViewModel
import com.smartsales.aitest.ui.theme.AiFeatureTestAppTheme
import com.smartsales.aitest.util.AudioFilePicker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AiTestViewModel

    // File picker launcher
    private val filePickerLauncher =
        registerForActivityResult(
            ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                lifecycleScope.launch {
                    val audioFile = AudioFilePicker.getFileFromUri(this@MainActivity, selectedUri)
                    if (audioFile != null) {
                        // Validate the file
                        val validationResult = AudioFilePicker.validateAudioFile(audioFile)
                        if (validationResult.isSuccess) {
                            viewModel.selectLocalAudioFile(audioFile)
                        } else {
                            // Handle validation error
                            val error = validationResult.exceptionOrNull()?.message ?: "文件验证失败"
                            // Could show a toast or update UI state
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AiFeatureTestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AiTestScreen(
                        onSelectLocalFile = {
                            filePickerLauncher.launch("audio/*")
                        },
                    )
                }
            }
        }
    }
}

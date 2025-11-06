package com.smartsales.wifibletest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import com.smartsales.wifibletest.ui.WifiBleTestScreen
import com.smartsales.wifibletest.ui.theme.WifiBleTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WifiBleTestTheme(useDarkTheme = isSystemInDarkTheme()) {
                Surface {
                    WifiBleTestScreen()
                }
            }
        }
    }
}

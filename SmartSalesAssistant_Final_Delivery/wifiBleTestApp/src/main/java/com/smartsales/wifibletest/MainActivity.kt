package com.smartsales.wifibletest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartsales.wifibletest.ui.WebConsoleScreen
import com.smartsales.wifibletest.ui.WifiBleTestScreen
import com.smartsales.wifibletest.ui.WifiBleTestViewModel
import com.smartsales.wifibletest.ui.theme.WifiBleTestTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private var hasPermissions by mutableStateOf(false)

    private val requiredPermissions: Array<String>
        get() {
            val permissions =
                mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions += Manifest.permission.BLUETOOTH_SCAN
                permissions += Manifest.permission.BLUETOOTH_CONNECT
            }

            return permissions.toTypedArray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { results ->
                hasPermissions = results.values.all { it }
            }

        hasPermissions = arePermissionsGranted()
        if (!hasPermissions) {
            requestRuntimePermissions()
        }

        setContent {
            WifiBleTestTheme(useDarkTheme = isSystemInDarkTheme()) {
                Surface {
                    val navController = rememberNavController()
                    val viewModel: WifiBleTestViewModel = hiltViewModel()

                    NavHost(
                        navController = navController,
                        startDestination = Destination.Dashboard.route,
                    ) {
                        composable(Destination.Dashboard.route) {
                            WifiBleTestScreen(
                                hasPermissions = hasPermissions,
                                onRequestPermissions = ::requestRuntimePermissions,
                                onNavigateToWebConsole = {
                                    navController.navigate(Destination.WebConsole.route)
                                },
                                viewModel = viewModel,
                            )
                        }
                        composable(Destination.WebConsole.route) {
                            WebConsoleScreen(
                                viewModel = viewModel,
                                onClose = {
                                    viewModel.closeWebConsole()
                                    navController.popBackStack()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private enum class Destination(val route: String) {
        Dashboard("dashboard"),
        WebConsole("web_console"),
    }

    private fun arePermissionsGranted(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRuntimePermissions() {
        if (!arePermissionsGranted()) {
            permissionLauncher.launch(requiredPermissions)
        } else {
            hasPermissions = true
        }
    }
}

package com.svcj91.naradavoicerecorder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.svcj91.naradavoicerecorder.presentation.PermissionHelper
import com.svcj91.naradavoicerecorder.presentation.navigation.AppNavHost
import com.svcj91.naradavoicerecorder.ui.theme.NaradaVoiceRecorderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NaradaVoiceRecorderTheme {
                val context = LocalContext.current
                var hasPermissions by remember {
                    mutableStateOf(PermissionHelper.hasAllRequiredPermissions(context))
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // At a minimum, RECORD_AUDIO is required to run the recorder.
                    val audioGranted = permissions[android.Manifest.permission.RECORD_AUDIO] == true
                    hasPermissions = audioGranted
                }

                // Check permissions on start
                LaunchedEffect(Unit) {
                    hasPermissions = PermissionHelper.hasRecordAudioPermission(context)
                }

                AppNavHost(
                    hasPermissions = hasPermissions,
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
package com.svcj91.naradavoicerecorder

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

                // Re-check permissions every time the Activity resumes (e.g. returning from Settings)
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            hasPermissions = PermissionHelper.hasRecordAudioPermission(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                // Check and request permissions on start
                LaunchedEffect(Unit) {
                    val hasAudio = PermissionHelper.hasRecordAudioPermission(context)
                    hasPermissions = hasAudio
                    if (!PermissionHelper.hasAllRequiredPermissions(context)) {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
                    }
                }

                val openSettings = {
                    val intent = Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }

                AppNavHost(
                    hasPermissions = hasPermissions,
                    onRequestPermissions = {
                        permissionLauncher.launch(PermissionHelper.getRequiredPermissions().toTypedArray())
                    },
                    onOpenSettings = openSettings,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
package com.svcj91.naradavoicerecorder.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Recorder : Screen("recorder", "Recorder", Icons.Rounded.Mic)
    object Recordings : Screen("recordings", "Recordings", Icons.Rounded.Folder)
}

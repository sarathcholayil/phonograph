package com.svcj91.naradavoicerecorder.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.svcj91.naradavoicerecorder.presentation.recorder.RecorderScreen
import com.svcj91.naradavoicerecorder.presentation.recorder.RecorderViewModel
import com.svcj91.naradavoicerecorder.presentation.recordings.RecordingsScreen
import com.svcj91.naradavoicerecorder.presentation.recordings.RecordingsViewModel
import com.svcj91.naradavoicerecorder.ui.theme.CoolGrayBlue
import com.svcj91.naradavoicerecorder.ui.theme.CoralRed
import com.svcj91.naradavoicerecorder.ui.theme.DarkSurface

@Composable
fun AppNavHost(
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Recorder,
        Screen.Recordings
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CoralRed,
                            selectedTextColor = CoralRed,
                            unselectedIconColor = CoolGrayBlue,
                            unselectedTextColor = CoolGrayBlue,
                            indicatorColor = Color(0xFF1D1F2E)
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Recorder.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Recorder.route) {
                val recorderViewModel: RecorderViewModel = hiltViewModel()
                RecorderScreen(
                    viewModel = recorderViewModel,
                    hasPermissions = hasPermissions,
                    onRequestPermissions = onRequestPermissions,
                    onOpenSettings = onOpenSettings
                )
            }
            composable(Screen.Recordings.route) {
                val recordingsViewModel: RecordingsViewModel = hiltViewModel()
                RecordingsScreen(
                    viewModel = recordingsViewModel
                )
            }
        }
    }
}

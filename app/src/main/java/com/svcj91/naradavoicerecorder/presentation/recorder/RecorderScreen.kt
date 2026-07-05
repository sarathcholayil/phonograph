package com.svcj91.naradavoicerecorder.presentation.recorder

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.svcj91.naradavoicerecorder.R
import com.svcj91.naradavoicerecorder.ui.theme.CoolGrayBlue
import com.svcj91.naradavoicerecorder.ui.theme.CoralRed
import com.svcj91.naradavoicerecorder.ui.theme.DarkBg
import com.svcj91.naradavoicerecorder.ui.theme.LightGrayBlue
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import android.os.PowerManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import androidx.core.app.ActivityCompat
import android.os.Build
import androidx.compose.material.icons.rounded.Notifications
import com.svcj91.naradavoicerecorder.presentation.PermissionHelper
import java.util.Locale
import kotlin.math.sin

@Composable
fun RecorderScreen(
    viewModel: RecorderViewModel,
    hasPermissions: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val elapsedTimeSeconds by viewModel.elapsedTimeSeconds.collectAsState()
    val recentCount by viewModel.recentRecordingsCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationRationaleDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showBatteryGuideDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isBatteryOptimizing by remember { mutableStateOf(false) }
    
    val sharedPrefs = remember {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    var isBatteryBannerDismissed by remember {
        mutableStateOf(sharedPrefs.getBoolean("battery_banner_dismissed", false))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(PermissionHelper.hasPostNotificationsPermission(context))
    }
    var isNotificationBannerDismissed by remember {
        mutableStateOf(sharedPrefs.getBoolean("notification_banner_dismissed", false))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                isBatteryOptimizing = pm?.isIgnoringBatteryOptimizations(context.packageName) == false
                hasNotificationPermission = PermissionHelper.hasPostNotificationsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Pulsing animation for the record button when recording and not paused
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val buttonScale by if (isRecording && !isPaused) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.12f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "buttonScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    // Pulse alpha for background glows when recording and not paused; static glow when paused
    val glowAlpha by if (isRecording && !isPaused) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else if (isRecording && isPaused) {
        remember { mutableStateOf(0.15f) }
    } else {
        remember { mutableStateOf(0f) }
    }

    // Pulsing animation for the resume button when paused
    val resumeButtonScale by if (isRecording && isPaused) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "resumeButtonScale"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = {
                Text(
                    text = "Microphone Access Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LightGrayBlue
                )
            },
            text = {
                Text(
                    text = "Phonograph requires access to the microphone to capture voice notes. Please allow access when prompted.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        onRequestPermissions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Grant", color = LightGrayBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel", color = CoolGrayBlue)
                }
            },
            containerColor = Color(0xFF2B2D42)
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = {
                Text(
                    text = "Microphone Access Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LightGrayBlue
                )
            },
            text = {
                Text(
                    text = "Phonograph requires access to the microphone to capture voice notes. Please authorize access in App Settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSettingsDialog = false
                        onOpenSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Settings", color = LightGrayBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = CoolGrayBlue)
                }
            },
            containerColor = Color(0xFF2B2D42)
        )
    }

    if (showNotificationRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationRationaleDialog = false },
            title = {
                Text(
                    text = "Notifications Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LightGrayBlue
                )
            },
            text = {
                Text(
                    text = "Phonograph requires notification permission to run the recording background service and show status updates. Please allow when prompted.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationRationaleDialog = false
                        onRequestPermissions()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Grant", color = LightGrayBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationRationaleDialog = false }) {
                    Text("Cancel", color = CoolGrayBlue)
                }
            },
            containerColor = Color(0xFF2B2D42)
        )
    }

    if (showNotificationSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationSettingsDialog = false },
            title = {
                Text(
                    text = "Notifications Required",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LightGrayBlue
                )
            },
            text = {
                Text(
                    text = "Phonograph requires notification permission to run the recording background service. Please authorize notifications in App Settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationSettingsDialog = false
                        onOpenSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Settings", color = LightGrayBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationSettingsDialog = false }) {
                    Text("Cancel", color = CoolGrayBlue)
                }
            },
            containerColor = Color(0xFF2B2D42)
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = {
                Text(
                    text = "Discard Recording?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LightGrayBlue
                )
            },
            text = {
                Text(
                    text = "This will permanently delete the current recording session.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.discardRecording()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Discard", color = LightGrayBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel", color = CoolGrayBlue)
                }
            },
            containerColor = Color(0xFF2B2D42)
        )
    }

    if (showBatteryGuideDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryGuideDialog = false },
            title = {
                Text(
                    text = "Battery Optimization",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = LightGrayBlue
                )
            },
            text = {
                Text(
                    text = getBatteryOptimizationInstructions(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryGuideDialog = false
                        onOpenSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703))
                ) {
                    Text("Go to Settings", color = DarkBg)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryGuideDialog = false }) {
                    Text("Cancel", color = CoolGrayBlue)
                }
            },
            containerColor = Color(0xFF2B2D42)
        )
    }

    Box(modifier = modifier.fillMaxSize().background(DarkBg)) {
        val scrollState = rememberScrollState()
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // heightIn(min = maxHeight) keeps the SpaceBetween layout spread out to
            // fill the screen when content fits, while verticalScroll lets it scroll
            // when content is taller than the screen (e.g. Display size = Large,
            // large font scale, or when several banners are shown at once).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .heightIn(min = maxHeight)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
            // Top Section: Title & Brand Info + Warning Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "Brand Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.2f))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PHONOGRAPH",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 24.sp,
                        letterSpacing = 4.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = LightGrayBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Voice Recorder",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )

                if (!hasPermissions) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = CoralRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.4f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Warning",
                                tint = CoralRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microphone Disabled",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LightGrayBlue
                                )
                                Text(
                                    text = "Grant permissions to record.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = CoolGrayBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val activity = context as? Activity
                                    val showRationale = activity?.let {
                                        ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.RECORD_AUDIO)
                                    } ?: false
                                    if (showRationale) {
                                        showRationaleDialog = true
                                    } else {
                                        showSettingsDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = "Enable",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LightGrayBlue
                                )
                            }
                        }
                    }
                }

                if (hasPermissions && isBatteryOptimizing && !isBatteryBannerDismissed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFFFFB703).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB703).copy(alpha = 0.4f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Battery Warning",
                                tint = Color(0xFFFFB703),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Battery Optimization Active",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LightGrayBlue
                                )
                                Text(
                                    text = "App may be terminated in background. Disable optimizations for uninterrupted recording.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = CoolGrayBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showBatteryGuideDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Disable",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = DarkBg
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        sharedPrefs.edit().putBoolean("battery_banner_dismissed", true).apply()
                                        isBatteryBannerDismissed = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 10.sp
                                        ),
                                        color = CoolGrayBlue
                                    )
                                }
                            }
                        }
                    }
                }

                if (hasPermissions && !hasNotificationPermission && !isNotificationBannerDismissed) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = Color(0xFF3A86FF).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A86FF).copy(alpha = 0.4f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Notifications,
                                contentDescription = "Notification Warning",
                                tint = Color(0xFF3A86FF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notifications Disabled",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = LightGrayBlue
                                )
                                Text(
                                    text = "Enable notifications to see status in the background.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = CoolGrayBlue
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val activity = context as? Activity
                                            val showRationale = activity?.let {
                                                ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.POST_NOTIFICATIONS)
                                            } ?: false
                                            if (showRationale) {
                                                showNotificationRationaleDialog = true
                                            } else {
                                                showNotificationSettingsDialog = true
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A86FF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = "Enable",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = LightGrayBlue
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        sharedPrefs.edit().putBoolean("notification_banner_dismissed", true).apply()
                                        isNotificationBannerDismissed = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text(
                                        text = "Dismiss",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 10.sp
                                        ),
                                        color = CoolGrayBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Middle Section: Waveform visualizer & Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                // Waveform visualizer
                AnimatedWaveform(isRecording = isRecording && !isPaused)

                Spacer(modifier = Modifier.height(16.dp))

                // Timer display
                Text(
                    text = formatElapsedTime(elapsedTimeSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    ),
                    color = LightGrayBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                // State status text
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isPaused) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CoolGrayBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PAUSED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = CoolGrayBlue
                            )
                        } else {
                            val statusDotAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.2f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 600, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "statusDotAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(CoralRed.copy(alpha = statusDotAlpha))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "RECORDING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = CoralRed
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (hasPermissions) "READY TO RECORD" else "MIC PERMISSION REQUIRED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = if (hasPermissions) CoolGrayBlue else CoralRed
                    )
                }
            }

            // Bottom Section: Record Button & Recent Counts
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Pause/Resume button slot
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            IconButton(
                                onClick = {
                                    if (isPaused) {
                                        viewModel.resumeRecording()
                                    } else {
                                        viewModel.pauseRecording()
                                    }
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .scale(resumeButtonScale)
                                    .clip(CircleShape)
                                    .background(if (isPaused) Color(0xFF2ECC71) else Color(0xFF2B2D42))
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = if (isPaused) DarkBg else LightGrayBlue,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    // Center: Record/Stop Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(160.dp)
                    ) {
                        // Outer glow effect
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(136.dp)
                                    .clip(CircleShape)
                                    .background(CoralRed.copy(alpha = glowAlpha))
                            )
                        }

                        // Main Record Button
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(buttonScale)
                                .clip(CircleShape)
                                .background(
                                    if (hasPermissions) {
                                        Brush.linearGradient(
                                            colors = listOf(CoralRed, Color(0xFFD90429))
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(CoolGrayBlue.copy(alpha = 0.4f), CoolGrayBlue.copy(alpha = 0.2f))
                                        )
                                    }
                                )
                                .clickable {
                                    if (!hasPermissions) {
                                        val activity = context as? Activity
                                        val showRationale = activity?.let {
                                            ActivityCompat.shouldShowRequestPermissionRationale(it, android.Manifest.permission.RECORD_AUDIO)
                                        } ?: false
                                        if (showRationale) {
                                            showRationaleDialog = true
                                        } else {
                                            showSettingsDialog = true
                                        }
                                    } else {
                                        if (isRecording) {
                                            viewModel.stopRecording()
                                        } else {
                                            viewModel.startRecording()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isRecording) {
                                // Stop Icon: Rounded square
                                Surface(
                                    modifier = Modifier.size(24.dp),
                                    shape = MaterialTheme.shapes.small,
                                    color = LightGrayBlue
                                ) {}
                            } else {
                                // Record Icon: Mic
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Start Recording",
                                    tint = if (hasPermissions) LightGrayBlue else LightGrayBlue.copy(alpha = 0.4f),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    // Right: Discard Button slot
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecording) {
                            IconButton(
                                onClick = { showDiscardDialog = true },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2B2D42))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Discard Recording",
                                    tint = CoralRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recent recordings indicator (only in idle state)
                if (!isRecording) {
                    Surface(
                        color = Color(0xFF2B2D42),
                        shape = CircleShape,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$recentCount recent recordings",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = LightGrayBlue.copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    // Dummy height to maintain layout spacing
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
            }
        }

        // Floating Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
fun AnimatedWaveform(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val waveOffset by if (isRecording) {
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 2f * Math.PI.toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "waveOffset"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val flatMultiplier by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flat"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .padding(horizontal = 24.dp)
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f
        val barCount = 45
        val barSpacing = 6.dp.toPx()
        val totalSpacing = barSpacing * (barCount - 1)
        val barWidth = (width - totalSpacing) / barCount

        for (i in 0 until barCount) {
            val fraction = i.toFloat() / barCount
            // Make wave taller in the center, flatter at edges
            val amplitudeFactor = sin(fraction * Math.PI.toFloat()) * height * 0.4f
            
            val waveValue = if (isRecording) {
                // Calculate sinusoidal heights
                sin(fraction * 4f * Math.PI.toFloat() + waveOffset) * amplitudeFactor * flatMultiplier
            } else {
                // Subtle static sound line in idle state
                (sin(fraction * 12f * Math.PI.toFloat()) * 4f)
            }

            val minHeight = 4.dp.toPx()
            val finalHeight = if (height > minHeight) {
                (2.dp.toPx() + Math.abs(waveValue)).coerceIn(minHeight, height)
            } else {
                height.coerceAtLeast(0f)
            }
            val top = midY - finalHeight / 2f
            val left = i * (barWidth + barSpacing)

            // Make the color gradient from center outwards
            val color = if (isRecording) {
                val colorFraction = Math.abs(fraction - 0.5f) * 2f
                lerp(CoralRed, CoolGrayBlue, colorFraction)
            } else {
                CoolGrayBlue.copy(alpha = 0.4f)
            }

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, finalHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

private fun getBatteryOptimizationInstructions(): String {
    val manufacturer = android.os.Build.MANUFACTURER.uppercase(Locale.US)
    return when {
        manufacturer.contains("ONEPLUS") || manufacturer.contains("OPPO") || manufacturer.contains("REALME") -> {
            "To ensure uninterrupted background recording on your device:\n\n" +
            "1. Tap 'Go to Settings' below (which opens the App Info page).\n" +
            "2. Tap 'Battery usage' (or 'Battery').\n" +
            "3. Enable the 'Allow background usage' toggle.\n\n" +
            "If already enabled, you can safely dismiss the banner."
        }
        manufacturer.contains("XIAOMI") || manufacturer.contains("REDMI") -> {
            "To ensure uninterrupted background recording on your device:\n\n" +
            "1. Tap 'Go to Settings' below.\n" +
            "2. Tap 'Battery saver' (or 'Battery').\n" +
            "3. Select 'No restrictions'."
        }
        manufacturer.contains("SAMSUNG") -> {
            "To ensure uninterrupted background recording on your device:\n\n" +
            "1. Tap 'Go to Settings' below.\n" +
            "2. Tap 'Battery'.\n" +
            "3. Select 'Unrestricted'."
        }
        manufacturer.contains("HUAWEI") -> {
            "To ensure uninterrupted background recording on your device:\n\n" +
            "1. Tap 'Go to Settings' below.\n" +
            "2. Tap 'Battery' -> 'App launch'.\n" +
            "3. Turn off 'Manage automatically' and enable 'Run in background'."
        }
        else -> {
            "To ensure uninterrupted background recording on your device:\n\n" +
            "1. Tap 'Go to Settings' below.\n" +
            "2. Tap 'Battery' or 'Battery usage'.\n" +
            "3. Select 'Unrestricted' or disable optimization."
        }
    }
}

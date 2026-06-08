package com.svcj91.naradavoicerecorder.presentation.recorder

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
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
import com.svcj91.naradavoicerecorder.ui.theme.CoolGrayBlue
import com.svcj91.naradavoicerecorder.ui.theme.CoralRed
import com.svcj91.naradavoicerecorder.ui.theme.DarkBg
import com.svcj91.naradavoicerecorder.ui.theme.LightGrayBlue
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
    val isRecording by viewModel.isRecording.collectAsState()
    val elapsedTimeSeconds by viewModel.elapsedTimeSeconds.collectAsState()
    val recentCount by viewModel.recentRecordingsCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRationaleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Pulsing animation for the record button when recording
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val buttonScale by if (isRecording) {
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

    // Pulse alpha for background glows
    val glowAlpha by if (isRecording) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.35f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
    } else {
        remember { mutableStateOf(0f) }
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
                    text = "Narada requires access to the microphone to capture voice notes. Please authorize access in App Settings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = CoolGrayBlue
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        onOpenSettings()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Settings", color = LightGrayBlue)
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Title & Brand Info + Warning Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Text(
                    text = "NARADA",
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
                                onClick = { showRationaleDialog = true },
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
            }

            // Middle Section: Waveform visualizer & Timer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Waveform visualizer
                AnimatedWaveform(isRecording = isRecording)

                Spacer(modifier = Modifier.height(32.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // State status text
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
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
                    .padding(bottom = 32.dp)
            ) {
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
                                    showRationaleDialog = true
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
            .height(120.dp)
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

            val finalHeight = (2.dp.toPx() + Math.abs(waveValue)).coerceIn(4.dp.toPx(), height)
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

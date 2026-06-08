package com.svcj91.naradavoicerecorder.presentation.recordings

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.svcj91.naradavoicerecorder.domain.model.PlaybackState
import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.ui.theme.CoolGrayBlue
import com.svcj91.naradavoicerecorder.ui.theme.CoralRed
import com.svcj91.naradavoicerecorder.ui.theme.DarkBg
import com.svcj91.naradavoicerecorder.ui.theme.DarkSurface
import com.svcj91.naradavoicerecorder.ui.theme.LightGrayBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(
    viewModel: RecordingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recordings by viewModel.recordings.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(horizontal = 16.dp)
    ) {
        // Title block
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "RECORDINGS",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 24.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            ),
            color = LightGrayBlue,
            modifier = Modifier.padding(start = 8.dp, bottom = 16.dp)
        )

        if (recordings.isEmpty()) {
            EmptyState()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(
                    items = recordings,
                    key = { it.id }
                ) { recording ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deleteRecording(recording)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.EndToStart -> CoralRed
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(color)
                                    .padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = LightGrayBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        enableDismissFromStartToEnd = false,
                        modifier = Modifier.animateItem()
                    ) {
                        RecordingCard(
                            recording = recording,
                            playbackState = playbackState,
                            onPlayPause = {
                                if (playbackState.activeUri == recording.uri && playbackState.isPlaying) {
                                    viewModel.pause()
                                } else {
                                    viewModel.play(recording)
                                }
                            },
                            onSeek = { position ->
                                viewModel.seekTo(position)
                            },
                            onShare = {
                                shareRecording(context, viewModel.getShareIntent(recording))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingCard(
    recording: Recording,
    playbackState: PlaybackState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActive = playbackState.activeUri == recording.uri
    val isPlaying = isActive && playbackState.isPlaying

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium, // 16dp Rounded Corners
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Play / Pause Circle Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) CoralRed else LightGrayBlue.copy(alpha = 0.08f)
                        )
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isActive) DarkBg else LightGrayBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Title & Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LightGrayBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatDate(recording.dateCreated),
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolGrayBlue
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(CoolGrayBlue.copy(alpha = 0.5f))
                        )
                        Text(
                            text = formatFileSize(recording.sizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = CoolGrayBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Actions: Duration & Share
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatDuration(recording.durationMs),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = LightGrayBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Share",
                            tint = CoolGrayBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Expandable Seekbar Container
            AnimatedVisibility(
                visible = isActive,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    var dragPosition by remember { mutableStateOf<Float?>(null) }
                    val maxPosition = if (playbackState.durationMs > 0) playbackState.durationMs.toFloat() else 100f
                    val currentSliderValue = (dragPosition ?: playbackState.currentPositionMs.toFloat()).coerceIn(0f, maxPosition)

                    Slider(
                        value = currentSliderValue,
                        onValueChange = {
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            dragPosition?.let {
                                onSeek(it.toLong())
                            }
                            dragPosition = null
                        },
                        valueRange = 0f..maxPosition,
                        colors = SliderDefaults.colors(
                            thumbColor = CoralRed,
                            activeTrackColor = CoralRed,
                            inactiveTrackColor = CoolGrayBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.height(20.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(currentSliderValue.toLong()),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CoolGrayBlue
                        )
                        Text(
                            text = formatDuration(playbackState.durationMs),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = CoolGrayBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            // Glowing circular background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CoolGrayBlue.copy(alpha = 0.05f))
            )
            // Custom Waveform static art
            Canvas(modifier = Modifier.size(80.dp)) {
                val width = size.width
                val height = size.height
                val midY = height / 2f
                val points = 15
                val spacing = 4.dp.toPx()
                val barWidth = (width - (spacing * (points - 1))) / points
                for (i in 0 until points) {
                    val hFactor = sin((i.toFloat() / points) * Math.PI.toFloat())
                    val barHeight = 8.dp.toPx() + (hFactor * height * 0.4f)
                    val top = midY - barHeight / 2f
                    val left = i * (barWidth + spacing)
                    drawRoundRect(
                        color = CoolGrayBlue.copy(alpha = 0.3f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No recordings yet",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = LightGrayBlue
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Your recordings will appear here",
            style = MaterialTheme.typography.bodyLarge,
            color = CoolGrayBlue
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return if (mb > 1.0) {
        String.format(Locale.US, "%.1f MB", mb)
    } else {
        String.format(Locale.US, "%.1f KB", kb)
    }
}

private fun formatDuration(ms: Long): String {
    val totalSecs = ms / 1000
    val m = totalSecs / 60
    val s = totalSecs % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}

private fun formatDate(timestampMs: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestampMs))
}

private fun shareRecording(context: Context, intent: Intent) {
    try {
        val chooser = Intent.createChooser(intent, "Share Recording")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

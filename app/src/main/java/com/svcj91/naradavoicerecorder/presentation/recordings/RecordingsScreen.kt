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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import androidx.compose.ui.text.style.TextAlign
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

    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var recordingToDelete by remember { mutableStateOf<Recording?>(null) }
    var recordingToRename by remember { mutableStateOf<Recording?>(null) }
    var renameNewText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.errorEvents.collect { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
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

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                placeholder = { Text("Search by name...", color = CoolGrayBlue.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = CoolGrayBlue
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = CoolGrayBlue
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = Color(0xFF4A90E2),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = LightGrayBlue,
                    unfocusedTextColor = LightGrayBlue,
                    cursorColor = Color(0xFF4A90E2)
                )
            )

            // Filter & Sort chips. Each chip takes half the row (weight(1f)) and the
            // labels drop the "Sort:"/"Date:" prefixes (the icons convey that), so
            // both chips stay on one line at any display size / font scale instead
            // of wrapping and breaking the layout.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort Chip Dropdown
                var sortMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    FilterChipButton(
                        label = sortOption.displayName,
                        icon = Icons.AutoMirrored.Rounded.Sort,
                        onClick = { sortMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        SortOption.values().forEach { option ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = option.displayName, 
                                        color = if (sortOption == option) Color(0xFF4A90E2) else LightGrayBlue
                                    ) 
                                },
                                onClick = {
                                    viewModel.setSortOption(option)
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Filter Chip Dropdown
                var dateMenuExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    FilterChipButton(
                        label = dateFilter.displayName,
                        icon = Icons.Rounded.DateRange,
                        onClick = { dateMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = dateMenuExpanded,
                        onDismissRequest = { dateMenuExpanded = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DateFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = filter.displayName, 
                                        color = if (dateFilter == filter) Color(0xFF4A90E2) else LightGrayBlue
                                    ) 
                                },
                                onClick = {
                                    viewModel.setDateFilter(filter)
                                    dateMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (recordings.isEmpty()) {
                val isFiltered = searchQuery.isNotEmpty() || dateFilter != DateFilter.ALL
                if (isFiltered) {
                    FilterEmptyState(
                        onClearFilters = {
                            viewModel.setSearchQuery("")
                            viewModel.setDateFilter(DateFilter.ALL)
                        }
                    )
                } else {
                    EmptyState()
                }
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
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        recordingToDelete = recording
                                        false
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        recordingToRename = recording
                                        renameNewText = recording.name.substringBeforeLast(".")
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val color = when (direction) {
                                    SwipeToDismissBoxValue.EndToStart -> CoralRed
                                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4A90E2)
                                    else -> Color.Transparent
                                }
                                val alignment = when (direction) {
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    else -> Alignment.Center
                                }
                                val icon = when (direction) {
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Rounded.Delete
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Rounded.Edit
                                    else -> Icons.Rounded.Delete
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(color)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = alignment
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = if (direction == SwipeToDismissBoxValue.StartToEnd) "Rename" else "Delete",
                                        tint = LightGrayBlue,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = true,
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

        // Floating Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        if (recordingToDelete != null) {
            AlertDialog(
                onDismissRequest = { recordingToDelete = null },
                title = {
                    Text(
                        text = "Delete Recording?",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LightGrayBlue
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to permanently delete \"${recordingToDelete?.name}\"?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CoolGrayBlue
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            recordingToDelete?.let { viewModel.deleteRecording(it) }
                            recordingToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                    ) {
                        Text("Delete", color = LightGrayBlue)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordingToDelete = null }) {
                        Text("Cancel", color = CoolGrayBlue)
                    }
                },
                containerColor = Color(0xFF2B2D42)
            )
        }

        if (recordingToRename != null) {
            AlertDialog(
                onDismissRequest = { recordingToRename = null },
                title = {
                    Text(
                        text = "Rename Recording",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = LightGrayBlue
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a new name for the recording:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CoolGrayBlue
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = renameNewText,
                            onValueChange = { renameNewText = it },
                            placeholder = { Text("Recording Name", color = CoolGrayBlue.copy(alpha = 0.5f)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = LightGrayBlue),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A90E2),
                                unfocusedBorderColor = CoolGrayBlue.copy(alpha = 0.5f),
                                focusedLabelColor = Color(0xFF4A90E2),
                                cursorColor = Color(0xFF4A90E2)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newNameTrimmed = renameNewText.trim()
                            if (newNameTrimmed.isNotEmpty()) {
                                val originalName = recordingToRename?.name ?: ""
                                val originalExt = originalName.substringAfterLast('.', "")
                                val ext = if (originalExt.isNotEmpty()) ".$originalExt" else ".m4a"
                                val finalName = if (newNameTrimmed.endsWith(ext, ignoreCase = true)) {
                                    newNameTrimmed
                                } else {
                                    newNameTrimmed + ext
                                }
                                recordingToRename?.let { viewModel.renameRecording(it, finalName) }
                            }
                            recordingToRename = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                    ) {
                        Text("Rename", color = LightGrayBlue)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordingToRename = null }) {
                        Text("Cancel", color = CoolGrayBlue)
                    }
                },
                containerColor = Color(0xFF2B2D42)
            )
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
                    val titleTime = remember(recording.dateCreated) {
                        SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(recording.dateCreated))
                    }
                    val subtitleDate = remember(recording.dateCreated) {
                        SimpleDateFormat("dd, MMM yyyy", Locale.US).format(Date(recording.dateCreated))
                    }

                    // Title: Recorded time in HH:MM:SS AM/PM
                    Text(
                        text = titleTime,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = LightGrayBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Subtitle: DD, MMM YYYY
                    Text(
                        text = subtitleDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolGrayBlue,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Final name: smaller than subtitle
                    Text(
                        text = recording.name,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp
                        ),
                        color = CoolGrayBlue.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Last row: File size
                    Text(
                        text = formatFileSize(recording.sizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = CoolGrayBlue
                    )
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

@Composable
fun FilterChipButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, CoolGrayBlue.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CoolGrayBlue,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = LightGrayBlue,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                tint = CoolGrayBlue,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun FilterEmptyState(
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(CoolGrayBlue.copy(alpha = 0.05f))
            )
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = CoolGrayBlue.copy(alpha = 0.3f),
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No matching recordings",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = LightGrayBlue
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Try adjusting your search query or filters",
            style = MaterialTheme.typography.bodyLarge,
            color = CoolGrayBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onClearFilters,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
        ) {
            Text("Clear Search & Filters", color = LightGrayBlue)
        }
    }
}

package com.svcj91.naradavoicerecorder.domain.model

import android.net.Uri
import kotlinx.coroutines.flow.Flow

/**
 * Data class representing the current audio playback state.
 */
data class PlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeUri: Uri? = null,
    val isCompleted: Boolean = false
)

/**
 * Domain interface defining audio playback capabilities.
 */
interface AudioPlayer {
    /**
     * Start or resume audio playback for the given [uri].
     */
    fun play(uri: Uri)

    /**
     * Pause the active audio playback.
     */
    fun pause()

    /**
     * Seek to the specified position in milliseconds.
     */
    fun seekTo(positionMs: Long)

    /**
     * Stop the current audio playback.
     */
    fun stop()

    /**
     * Release all player resources. After calling this, the player must be
     * re-created before it can be used again.
     */
    fun release()

    /**
     * Flow emitting updates to the current [PlaybackState].
     */
    val playbackState: Flow<PlaybackState>

    /**
     * Flow emitting error messages encountered during audio playback.
     */
    val errorEvents: Flow<String>
}

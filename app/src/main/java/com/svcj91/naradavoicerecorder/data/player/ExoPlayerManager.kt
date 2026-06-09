package com.svcj91.naradavoicerecorder.data.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import com.svcj91.naradavoicerecorder.domain.model.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Media3 ExoPlayer implementation of [AudioPlayer].
 * Handles audio playback thread safety by running player actions on [Dispatchers.Main].
 */
@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioPlayer {

    private val _playbackState = MutableStateFlow(PlaybackState())
    override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

    private val _errorEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)
    override val errorEvents: Flow<String> = _errorEvents

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var player: ExoPlayer? = null
    private var activeUri: Uri? = null
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            Log.d(TAG, "onPlaybackStateChanged: $playbackState")
            updateState()
            if (playbackState == Player.STATE_ENDED) {
                stopProgressUpdates()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: $isPlaying")
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
            }
            updateState()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "ExoPlayer error: ${error.message}", error)
            _errorEvents.tryEmit("Failed to play audio: file may be corrupt or deleted")
            stop()
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        val p = player
        if (p != null) return p

        Log.d(TAG, "Initializing ExoPlayer")
        val newPlayer = ExoPlayer.Builder(context).build().apply {
            addListener(playerListener)
        }
        player = newPlayer
        return newPlayer
    }

    override fun play(uri: Uri) {
        mainScope.launch {
            try {
                val currentPlayer = getOrCreatePlayer()
                if (activeUri != uri) {
                    Log.d(TAG, "Playing new URI: $uri. Stopping previous playback.")
                    currentPlayer.stop()
                    activeUri = uri
                    val mediaItem = MediaItem.fromUri(uri)
                    currentPlayer.setMediaItem(mediaItem)
                    currentPlayer.prepare()
                } else if (currentPlayer.playbackState == Player.STATE_ENDED) {
                    Log.d(TAG, "Replaying completed recording from beginning.")
                    currentPlayer.seekTo(0L)
                }
                currentPlayer.play()
                updateState()
            } catch (e: Exception) {
                Log.e(TAG, "Error in play", e)
                _errorEvents.tryEmit("Failed to play recording: ${e.localizedMessage ?: "unknown error"}")
            }
        }
    }

    override fun pause() {
        mainScope.launch {
            try {
                player?.pause()
                updateState()
            } catch (e: Exception) {
                Log.e(TAG, "Error in pause", e)
            }
        }
    }

    override fun seekTo(positionMs: Long) {
        mainScope.launch {
            try {
                player?.seekTo(positionMs)
                updateState()
            } catch (e: Exception) {
                Log.e(TAG, "Error in seekTo", e)
            }
        }
    }

    override fun stop() {
        mainScope.launch {
            try {
                Log.d(TAG, "Stopping playback and clearing active URI")
                player?.stop()
                activeUri = null
                stopProgressUpdates()
                updateState()
            } catch (e: Exception) {
                Log.e(TAG, "Error in stop", e)
            }
        }
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = mainScope.launch {
            while (isActive) {
                updateState()
                delay(250) // Update progress every 250ms
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updateState() {
        val currentPlayer = player
        if (currentPlayer == null) {
            _playbackState.value = PlaybackState()
            return
        }

        val duration = if (currentPlayer.duration >= 0) currentPlayer.duration else 0L
        val position = if (currentPlayer.currentPosition >= 0) currentPlayer.currentPosition else 0L
        val completed = currentPlayer.playbackState == Player.STATE_ENDED

        _playbackState.value = PlaybackState(
            isPlaying = currentPlayer.isPlaying,
            currentPositionMs = position,
            durationMs = duration,
            activeUri = activeUri,
            isCompleted = completed
        )
    }

    /**
     * Release player resources. The player will be lazily re-created by
     * [getOrCreatePlayer] on the next [play] call.
     */
    override fun release() {
        mainScope.launch {
            try {
                Log.d(TAG, "Releasing ExoPlayer resources")
                stopProgressUpdates()
                player?.removeListener(playerListener)
                player?.release()
                player = null
                activeUri = null
                updateState()
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing ExoPlayer", e)
            }
        }
    }

    companion object {
        private const val TAG = "ExoPlayerManager"
    }
}

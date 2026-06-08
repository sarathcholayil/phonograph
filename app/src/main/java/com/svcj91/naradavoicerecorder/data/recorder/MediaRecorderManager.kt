package com.svcj91.naradavoicerecorder.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of [AudioRecorder] using [MediaRecorder].
 */
@Singleton
class MediaRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioRecorder {

    private val _isRecording = MutableStateFlow(false)
    override val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    override val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    override val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private val _errorEvents = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 5)
    override val errorEvents: Flow<String> = _errorEvents

    private var recorder: MediaRecorder? = null
    private var timerJob: Job? = null
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private fun createMediaRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    override fun start(outputFile: File) {
        if (_isRecording.value) {
            Log.w(TAG, "Recording is already active. Ignoring start request.")
            return
        }

        try {
            // Check storage availability & write protection
            val parent = outputFile.parentFile
            if (parent == null) {
                throw java.io.IOException("Invalid output directory path")
            }
            if (!parent.exists() && !parent.mkdirs()) {
                throw java.io.IOException("Storage directory is write-protected or unmounted")
            }
            if (parent.freeSpace < 5 * 1024 * 1024) { // Less than 5 MB
                throw java.io.IOException("ENOSPC: Storage is full")
            }

            recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaRecorder error: what=$what, extra=$extra")
                    _errorEvents.tryEmit("Recording failed due to hardware/system error")
                    stop()
                }
                
                setOnInfoListener { _, what, extra ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
                        _errorEvents.tryEmit("Storage limit reached")
                        stop()
                    }
                }
                
                prepare()
                start()
            }
            _isRecording.value = true
            _isPaused.value = false
            startTimer()
            Log.d(TAG, "Recording started successfully. Saving to: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            val errorMsg = when {
                e is java.io.FileNotFoundException -> "Storage directory is write-protected or unmounted"
                e.message?.contains("ENOSPC") == true -> "Storage is full"
                e.message?.contains("write-protected") == true -> "Storage directory is write-protected"
                else -> "Microphone is busy or failed to initialize"
            }
            _errorEvents.tryEmit(errorMsg)
            releaseRecorder()
            _isRecording.value = false
            _isPaused.value = false
            throw e
        }
    }

    override fun stop() {
        if (!_isRecording.value) {
            Log.w(TAG, "No active recording session to stop.")
            return
        }

        try {
            recorder?.apply {
                stop()
            }
            Log.d(TAG, "Recording stopped successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaRecorder", e)
        } finally {
            stopTimer()
            releaseRecorder()
            _isRecording.value = false
            _isPaused.value = false
        }
    }

    override fun pause() {
        if (!_isRecording.value || _isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder?.pause()
                _isPaused.value = true
                Log.d(TAG, "Recording paused successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing MediaRecorder", e)
        }
    }

    override fun resume() {
        if (!_isRecording.value || !_isPaused.value) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                recorder?.resume()
                _isPaused.value = false
                Log.d(TAG, "Recording resumed successfully.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming MediaRecorder", e)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedTimeSeconds.value = 0L
        timerJob = managerScope.launch {
            while (isActive) {
                delay(1000)
                if (!_isPaused.value) {
                    _elapsedTimeSeconds.value += 1
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        _elapsedTimeSeconds.value = 0L
    }

    private fun releaseRecorder() {
        try {
            recorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaRecorder", e)
        } finally {
            recorder = null
        }
    }

    companion object {
        private const val TAG = "MediaRecorderManager"
    }
}

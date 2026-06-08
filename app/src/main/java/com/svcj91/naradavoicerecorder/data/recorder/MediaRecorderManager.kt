package com.svcj91.naradavoicerecorder.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    override val isRecording: Flow<Boolean> = _isRecording.asStateFlow()

    private var recorder: MediaRecorder? = null

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
            // Ensure parent directory exists
            outputFile.parentFile?.let { parent ->
                if (!parent.exists()) {
                    parent.mkdirs()
                }
            }

            recorder = createMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            Log.d(TAG, "Recording started successfully. Saving to: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio recording", e)
            releaseRecorder()
            _isRecording.value = false
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
            releaseRecorder()
            _isRecording.value = false
        }
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

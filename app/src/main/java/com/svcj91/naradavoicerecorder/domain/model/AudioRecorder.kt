package com.svcj91.naradavoicerecorder.domain.model

import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain interface defining audio recording capabilities.
 */
interface AudioRecorder {
    /**
     * Start audio recording, saving the output to the specified [outputFile].
     * Handle errors safely and transition state appropriately.
     */
    fun start(outputFile: File)

    /**
     * Stop the current audio recording session.
     */
    fun stop()

    /**
     * Pause the current audio recording session.
     */
    fun pause()

    /**
     * Resume the current audio recording session.
     */
    fun resume()

    /**
     * Flow emitting the current recording state (true if recording, false otherwise).
     */
    val isRecording: StateFlow<Boolean>

    /**
     * Flow emitting the current paused state (true if paused, false otherwise).
     */
    val isPaused: StateFlow<Boolean>

    /**
     * Flow emitting the elapsed recording time in seconds.
     */
    val elapsedTimeSeconds: StateFlow<Long>

    /**
     * Flow emitting error messages encountered by the recorder.
     */
    val errorEvents: Flow<String>
}

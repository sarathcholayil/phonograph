package com.svcj91.naradavoicerecorder.domain.model

import java.io.File
import kotlinx.coroutines.flow.Flow

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
     * Flow emitting the current recording state (true if recording, false otherwise).
     */
    val isRecording: Flow<Boolean>

    /**
     * Flow emitting error messages encountered by the recorder.
     */
    val errorEvents: Flow<String>
}

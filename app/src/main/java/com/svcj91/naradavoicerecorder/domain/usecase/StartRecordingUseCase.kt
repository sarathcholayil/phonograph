package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import java.io.File
import javax.inject.Inject

/**
 * Use case to start audio recording.
 */
class StartRecordingUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder
) {
    operator fun invoke(outputFile: File) {
        audioRecorder.start(outputFile)
    }
}

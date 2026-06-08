package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import javax.inject.Inject

/**
 * Use case to stop audio recording.
 */
class StopRecordingUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder
) {
    operator fun invoke() {
        audioRecorder.stop()
    }
}

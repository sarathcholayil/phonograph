package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the recording state.
 */
class GetRecordingStateUseCase @Inject constructor(
    private val audioRecorder: AudioRecorder
) {
    operator fun invoke(): Flow<Boolean> {
        return audioRecorder.isRecording
    }
}

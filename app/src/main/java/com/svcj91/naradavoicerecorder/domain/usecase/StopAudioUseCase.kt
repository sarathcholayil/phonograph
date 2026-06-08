package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import javax.inject.Inject

/**
 * Use case to stop active audio playback.
 */
class StopAudioUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    operator fun invoke() {
        audioPlayer.stop()
    }
}

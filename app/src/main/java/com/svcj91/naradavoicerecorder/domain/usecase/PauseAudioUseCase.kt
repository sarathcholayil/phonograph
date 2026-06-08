package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import javax.inject.Inject

/**
 * Use case to pause the active audio playback.
 */
class PauseAudioUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    operator fun invoke() {
        audioPlayer.pause()
    }
}

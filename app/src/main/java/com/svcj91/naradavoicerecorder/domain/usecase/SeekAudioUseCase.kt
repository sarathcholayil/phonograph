package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import javax.inject.Inject

/**
 * Use case to seek to a specific position in milliseconds within the active audio.
 */
class SeekAudioUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    operator fun invoke(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }
}

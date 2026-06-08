package com.svcj91.naradavoicerecorder.domain.usecase

import android.net.Uri
import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import javax.inject.Inject

/**
 * Use case to start or resume playback of an audio Uri.
 */
class PlayAudioUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    operator fun invoke(uri: Uri) {
        audioPlayer.play(uri)
    }
}

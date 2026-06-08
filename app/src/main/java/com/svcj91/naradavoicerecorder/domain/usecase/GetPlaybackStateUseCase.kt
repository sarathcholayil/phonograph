package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import com.svcj91.naradavoicerecorder.domain.model.PlaybackState
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the audio playback state.
 */
class GetPlaybackStateUseCase @Inject constructor(
    private val audioPlayer: AudioPlayer
) {
    operator fun invoke(): Flow<PlaybackState> {
        return audioPlayer.playbackState
    }
}

package com.svcj91.naradavoicerecorder.di

import com.svcj91.naradavoicerecorder.data.player.ExoPlayerManager
import com.svcj91.naradavoicerecorder.domain.model.AudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing dependencies related to the audio playback engine.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(
        exoPlayerManager: ExoPlayerManager
    ): AudioPlayer
}

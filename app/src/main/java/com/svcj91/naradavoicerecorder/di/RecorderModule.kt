package com.svcj91.naradavoicerecorder.di

import com.svcj91.naradavoicerecorder.data.recorder.MediaRecorderManager
import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecorderModule {

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(
        mediaRecorderManager: MediaRecorderManager
    ): AudioRecorder
}

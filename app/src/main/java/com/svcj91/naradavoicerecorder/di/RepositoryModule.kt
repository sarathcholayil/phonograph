package com.svcj91.naradavoicerecorder.di

import com.svcj91.naradavoicerecorder.data.repository.MediaStoreRepository
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecordingRepository(
        mediaStoreRepository: MediaStoreRepository
    ): RecordingRepository
}

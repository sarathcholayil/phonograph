package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case to delete a specific audio recording.
 */
class DeleteRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recording: Recording): Boolean {
        return repository.deleteRecording(recording)
    }
}

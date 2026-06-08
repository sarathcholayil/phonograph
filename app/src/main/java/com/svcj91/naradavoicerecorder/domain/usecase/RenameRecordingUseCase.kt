package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case to rename a specific audio recording.
 */
class RenameRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    suspend operator fun invoke(recording: Recording, newName: String): Boolean {
        return repository.renameRecording(recording, newName)
    }
}

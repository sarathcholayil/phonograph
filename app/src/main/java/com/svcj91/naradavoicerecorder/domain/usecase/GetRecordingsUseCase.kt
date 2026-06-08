package com.svcj91.naradavoicerecorder.domain.usecase

import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to retrieve the flow of all audio recordings.
 */
class GetRecordingsUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    operator fun invoke(): Flow<List<Recording>> {
        return repository.getRecordings()
    }
}

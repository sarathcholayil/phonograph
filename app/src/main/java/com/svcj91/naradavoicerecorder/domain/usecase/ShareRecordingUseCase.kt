package com.svcj91.naradavoicerecorder.domain.usecase

import android.content.Intent
import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import javax.inject.Inject

/**
 * Use case to generate a sharing Intent for an audio recording.
 */
class ShareRecordingUseCase @Inject constructor(
    private val repository: RecordingRepository
) {
    operator fun invoke(recording: Recording): Intent {
        return repository.getShareIntent(recording)
    }
}

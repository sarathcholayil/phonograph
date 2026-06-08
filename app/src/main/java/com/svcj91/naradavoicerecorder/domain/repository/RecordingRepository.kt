package com.svcj91.naradavoicerecorder.domain.repository

import android.content.Intent
import com.svcj91.naradavoicerecorder.domain.model.Recording
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Repository interface for managing audio recordings.
 */
interface RecordingRepository {
    /**
     * Retrieves a flow of all recorded audio files.
     */
    fun getRecordings(): Flow<List<Recording>>

    /**
     * Deletes the specified recording.
     * Returns true if deletion was successful, false otherwise.
     */
    suspend fun deleteRecording(recording: Recording): Boolean

    /**
     * Returns an Intent to share the specified recording.
     */
    fun getShareIntent(recording: Recording): Intent

    /**
     * Creates a temporary file to record audio into before publishing it.
     */
    fun createTempFile(): File

    /**
     * Publishes a temporary audio recording file to the Android MediaStore.
     * Returns the published Recording object, or null if publication failed.
     */
    suspend fun publishRecording(tempFile: File): Recording?
}

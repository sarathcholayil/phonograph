package com.svcj91.naradavoicerecorder.domain.model

import android.net.Uri

/**
 * Data class representing an audio recording.
 */
data class Recording(
    val id: Long,
    val name: String,
    val uri: Uri,
    val dateCreated: Long,
    val durationMs: Long,
    val sizeBytes: Long
)

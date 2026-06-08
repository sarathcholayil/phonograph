package com.svcj91.naradavoicerecorder.data.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.svcj91.naradavoicerecorder.domain.model.Recording
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of [RecordingRepository] using Android [MediaStore].
 */
@Singleton
class MediaStoreRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : RecordingRepository {

    override fun getRecordings(): Flow<List<Recording>> = callbackFlow {
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                trySend(queryRecordingsList())
            }
        }

        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )

        // Initial query
        trySend(queryRecordingsList())

        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override fun createTempFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(context.cacheDir, "temp_recording_$timeStamp.m4a")
    }

    override suspend fun publishRecording(tempFile: File): Recording? = withContext(Dispatchers.IO) {
        if (!tempFile.exists() || tempFile.length() == 0L) {
            Log.e(TAG, "Temp file does not exist or is empty: ${tempFile.absolutePath}")
            return@withContext null
        }

        val durationMs = try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(tempFile.absolutePath)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract duration from temp file", e)
            0L
        }

        val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val displayName = "$timeStamp.m4a"

        val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Environment.DIRECTORY_RECORDINGS + "/VoiceNotesRecorder"
        } else {
            Environment.DIRECTORY_MUSIC + "/VoiceNotesRecorder"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.SIZE, tempFile.length())
            put(MediaStore.Audio.Media.DURATION, durationMs)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    "VoiceNotesRecorder"
                )
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val destFile = File(dir, displayName)
                put(MediaStore.Audio.Media.DATA, destFile.absolutePath)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: return@withContext null

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                tempFile.inputStream().use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(uri, updateValues, null, null)
            }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            return@withContext queryRecordingByUri(uri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write temp file to MediaStore URI", e)
            resolver.delete(uri, null, null)
            null
        }
    }

    override suspend fun deleteRecording(recording: Recording): Boolean = withContext(Dispatchers.IO) {
        try {
            val rowsDeleted = context.contentResolver.delete(recording.uri, null, null)
            rowsDeleted > 0
        } catch (securityException: SecurityException) {
            Log.e(TAG, "SecurityException deleting recording: ${recording.uri}", securityException)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete recording: ${recording.uri}", e)
            false
        }
    }

    override fun getShareIntent(recording: Recording): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, recording.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun queryRecordingsList(): List<Recording> {
        val recordings = mutableListOf<Recording>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        val selection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? AND ${MediaStore.Audio.Media.IS_PENDING} = 0"
        } else {
            "${MediaStore.Audio.Media.DATA} LIKE ?"
        }
        val selectionArgs = arrayOf("%VoiceNotesRecorder%")
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    // MediaStore DATE_ADDED is in seconds, convert to milliseconds
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                    recordings.add(
                        Recording(
                            id = id,
                            name = name,
                            uri = uri,
                            dateCreated = dateAdded,
                            durationMs = duration,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying recordings from MediaStore", e)
        }

        return recordings
    }

    private fun queryRecordingByUri(uri: Uri): Recording? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Unknown"
                    val dateAdded = cursor.getLong(dateAddedColumn) * 1000
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)

                    return Recording(
                        id = id,
                        name = name,
                        uri = uri,
                        dateCreated = dateAdded,
                        durationMs = duration,
                        sizeBytes = size
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying recording by Uri: $uri", e)
        }
        return null
    }

    companion object {
        private const val TAG = "MediaStoreRepository"
    }
}

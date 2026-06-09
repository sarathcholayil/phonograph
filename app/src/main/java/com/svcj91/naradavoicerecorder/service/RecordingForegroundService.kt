package com.svcj91.naradavoicerecorder.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.svcj91.naradavoicerecorder.MainActivity
import com.svcj91.naradavoicerecorder.domain.model.AudioRecorder
import com.svcj91.naradavoicerecorder.domain.repository.RecordingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import javax.inject.Inject

/**
 * Foreground Service that handles background audio recording.
 */
@AndroidEntryPoint
class RecordingForegroundService : Service() {

    @Inject
    lateinit var audioRecorder: AudioRecorder

    @Inject
    lateinit var recordingRepository: RecordingRepository

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var currentTempFile: File? = null
    private var isServiceRunning = false

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "Service onBind")
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        registerReceiverCompat()

        // Observe elapsed time to update notification
        serviceScope.launch {
            audioRecorder.elapsedTimeSeconds.collect { elapsedSeconds ->
                if (isServiceRunning) {
                    updateNotification(elapsedSeconds)
                }
            }
        }

        // Observe paused state to update notification title/details
        serviceScope.launch {
            audioRecorder.isPaused.collect {
                if (isServiceRunning) {
                    updateNotification(audioRecorder.elapsedTimeSeconds.value)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> startRecordingService()
            ACTION_STOP -> stopRecordingService()
            ACTION_DISCARD -> discardRecordingService()
            ACTION_PAUSE -> pauseRecordingService()
            ACTION_RESUME -> resumeRecordingService()
        }
        return START_NOT_STICKY
    }

    private fun startRecordingService() {
        if (isServiceRunning) {
            Log.d(TAG, "Recording service already running")
            return
        }
        isServiceRunning = true

        try {
            val tempFile = recordingRepository.createTempFile()
            currentTempFile = tempFile
            audioRecorder.start(tempFile)

            val notification = buildNotification(0L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            Log.d(TAG, "Recording service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording service", e)
            stopRecordingService()
        }
    }

    private fun stopRecordingService() {
        if (!isServiceRunning) return
        isServiceRunning = false
        Log.d(TAG, "Stopping recording service")
        stopRecordingAndCleanup()
        stopSelf()
    }

    private fun discardRecordingService() {
        if (!isServiceRunning) return
        isServiceRunning = false
        Log.d(TAG, "Discarding recording service")
        discardRecordingAndCleanup()
        stopSelf()
    }

    private fun pauseRecordingService() {
        audioRecorder.pause()
    }

    private fun resumeRecordingService() {
        audioRecorder.resume()
    }

    private fun stopRecordingAndCleanup() {
        try {
            audioRecorder.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recorder", e)
        }

        val tempFile = currentTempFile
        currentTempFile = null

        if (tempFile != null && tempFile.exists()) {
            serviceScope.launch {
                withContext(Dispatchers.IO + NonCancellable) {
                    try {
                        val recording = recordingRepository.publishRecording(tempFile)
                        Log.d(TAG, "Recording published: $recording")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error publishing recording", e)
                    }
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun discardRecordingAndCleanup() {
        try {
            audioRecorder.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recorder", e)
        }

        val tempFile = currentTempFile
        currentTempFile = null

        if (tempFile != null && tempFile.exists()) {
            try {
                val deleted = tempFile.delete()
                Log.d(TAG, "Temp file deleted: $deleted")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting temp file", e)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun updateNotification(elapsedSeconds: Long) {
        val notification = buildNotification(elapsedSeconds)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(elapsedSeconds: Long): Notification {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val clickIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, clickIntent, pendingIntentFlags)

        // Action intents for Notification controls
        val stopServiceIntent = Intent(this, RecordingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(this, 1, stopServiceIntent, pendingIntentFlags)

        val isPaused = audioRecorder.isPaused.value
        val pauseResumeActionIntent = if (isPaused) {
            Intent(this, RecordingForegroundService::class.java).apply { action = ACTION_RESUME }
        } else {
            Intent(this, RecordingForegroundService::class.java).apply { action = ACTION_PAUSE }
        }
        val pauseResumePendingIntent = PendingIntent.getService(this, 2, pauseResumeActionIntent, pendingIntentFlags)

        val formattedTime = formatElapsedTime(elapsedSeconds)
        val title = if (isPaused) "Recording paused" else "Recording in progress"
        
        val pauseResumeIcon = if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        val pauseResumeText = if (isPaused) "Resume" else "Pause"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(formattedTime)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(pauseResumeIcon, pauseResumeText, pauseResumePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    private fun formatElapsedTime(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Channel for background audio recording service"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_STOP) {
                stopRecordingService()
            }
        }
    }

    private fun registerReceiverCompat() {
        val filter = IntentFilter(ACTION_STOP)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(stopReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(stopReceiver, filter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service onDestroy")
        try {
            unregisterReceiver(stopReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        if (isServiceRunning) {
            isServiceRunning = false
            stopRecordingAndCleanup()
        }
        serviceJob.cancel()
    }

    companion object {
        private const val TAG = "RecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "recording_channel"

        const val ACTION_START = "com.svcj91.naradavoicerecorder.service.ACTION_START"
        const val ACTION_STOP = "com.svcj91.naradavoicerecorder.service.ACTION_STOP"
        const val ACTION_DISCARD = "com.svcj91.naradavoicerecorder.service.ACTION_DISCARD"
        const val ACTION_PAUSE = "com.svcj91.naradavoicerecorder.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.svcj91.naradavoicerecorder.service.ACTION_RESUME"

        fun startService(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun discardService(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_DISCARD
            }
            context.startService(intent)
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }
    }
}

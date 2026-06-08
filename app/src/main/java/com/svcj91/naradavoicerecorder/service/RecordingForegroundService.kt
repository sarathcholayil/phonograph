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
import android.os.Binder
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private val _elapsedTimeSeconds = MutableStateFlow(0L)
    val elapsedTimeSeconds: StateFlow<Long> = _elapsedTimeSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var currentTempFile: File? = null
    private var isServiceRunning = false

    private val notificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RecordingForegroundService = this@RecordingForegroundService
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        registerReceiverCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand action: ${intent?.action}")
        when (intent?.action) {
            ACTION_START -> startRecordingService()
            ACTION_STOP -> stopRecordingService()
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

            startTimer()
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

    private fun stopRecordingAndCleanup() {
        stopTimer()
        try {
            audioRecorder.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recorder", e)
        }

        val tempFile = currentTempFile
        currentTempFile = null

        if (tempFile != null && tempFile.exists()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val recording = recordingRepository.publishRecording(tempFile)
                    Log.d(TAG, "Recording published: $recording")
                } catch (e: Exception) {
                    Log.e(TAG, "Error publishing recording", e)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _elapsedTimeSeconds.value = 0L
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                _elapsedTimeSeconds.value += 1
                updateNotification(_elapsedTimeSeconds.value)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
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

        val formattedTime = formatElapsedTime(elapsedSeconds)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording in progress")
            .setContentText(formattedTime)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
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
    }
}

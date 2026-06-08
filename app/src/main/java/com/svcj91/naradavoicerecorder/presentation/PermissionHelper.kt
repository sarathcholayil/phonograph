package com.svcj91.naradavoicerecorder.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility helper to manage and check runtime and manifest permissions required
 * for voice recording, notifications, and foreground services.
 */
object PermissionHelper {

    /**
     * Returns the list of permissions that need to be requested at runtime.
     * includes [Manifest.permission.RECORD_AUDIO] for all supported API levels,
     * and [Manifest.permission.POST_NOTIFICATIONS] for Android 13+ (API 33+).
     */
    fun getRequiredPermissions(): List<String> {
        return buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /**
     * Checks if a specific permission is granted.
     */
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Checks if all required runtime permissions are granted.
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return getRequiredPermissions().all { isPermissionGranted(context, it) }
    }

    /**
     * Checks if microphone permission (RECORD_AUDIO) is granted.
     */
    fun hasRecordAudioPermission(context: Context): Boolean {
        return isPermissionGranted(context, Manifest.permission.RECORD_AUDIO)
    }

    /**
     * Checks if post notifications permission (POST_NOTIFICATIONS) is granted.
     * On Android versions before 13 (Tiramisu), notifications do not require runtime permission,
     * so this returns true.
     */
    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPermissionGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    }

    /**
     * Checks if foreground service permission (FOREGROUND_SERVICE) is granted.
     * This is a normal permission and is granted at install time if declared in AndroidManifest.xml.
     * On Android versions before Pie (Android 9), this permission does not exist, so it returns true.
     */
    fun hasForegroundServicePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            isPermissionGranted(context, Manifest.permission.FOREGROUND_SERVICE)
        } else {
            true
        }
    }

    /**
     * Checks if foreground service microphone permission (FOREGROUND_SERVICE_MICROPHONE) is granted.
     * This is a normal permission and is granted at install time if declared in AndroidManifest.xml.
     * On Android versions before Upside Down Cake (Android 14), this permission does not exist, so it returns true.
     */
    fun hasForegroundServiceMicrophonePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            isPermissionGranted(context, Manifest.permission.FOREGROUND_SERVICE_MICROPHONE)
        } else {
            true
        }
    }
}

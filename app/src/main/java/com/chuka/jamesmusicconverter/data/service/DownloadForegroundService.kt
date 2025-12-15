package com.chuka.jamesmusicconverter.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.chuka.jamesmusicconverter.MainActivity

/**
 * Simple foreground service that keeps the app process alive during downloads
 * This ensures downloads continue even when the app is backgrounded
 */
class DownloadForegroundService : Service() {

    companion object {
        private const val TAG = "CHUKA_DownloadService"
        private const val CHANNEL_ID = "download_progress"
        private const val CHANNEL_NAME = "Download Progress"
        private const val NOTIFICATION_ID = 1000

        const val ACTION_START = "START"
        const val ACTION_STOP = "STOP"
        const val ACTION_UPDATE_PROGRESS = "UPDATE_PROGRESS"
        const val EXTRA_MESSAGE = "MESSAGE"
        const val EXTRA_PROGRESS = "PROGRESS"

        /**
         * Start the foreground service
         */
        fun startService(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Update the progress notification
         */
        fun updateProgress(context: Context, message: String, progress: Float) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_UPDATE_PROGRESS
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_PROGRESS, progress)
            }
            context.startService(intent)
        }

        /**
         * Stop the foreground service
         */
        fun stopService(context: Context) {
            val intent = Intent(context, DownloadForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentMessage = "Initializing..."
    private var currentProgress = 0f

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createProgressNotification(currentMessage, currentProgress))
            }
            ACTION_UPDATE_PROGRESS -> {
                currentMessage = intent.getStringExtra(EXTRA_MESSAGE) ?: "Downloading..."
                currentProgress = intent.getFloatExtra(EXTRA_PROGRESS, 0f)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(NOTIFICATION_ID, createProgressNotification(currentMessage, currentProgress))
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }

        return START_STICKY
    }

    /**
     * Create notification channel for foreground service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows download progress for music conversion"
                setSound(null, null)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Create progress notification
     */
    private fun createProgressNotification(message: String, progress: Float): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressPercent = (progress * 100).toInt()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Converting Media")
            .setContentText(message)
            .setSmallIcon(com.chuka.jamesmusicconverter.R.drawable.ic_notification_music)
            .setColor(0xFF607D8B.toInt()) // Muted slate blue to match launcher icon
            .setProgress(100, progressPercent, progress == 0f)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setSound(null)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}

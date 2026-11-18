package com.example.scenicguide.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.scenicguide.MainActivity
import com.example.scenicguide.R

/**
 * 录音和语音播报期间的前台服务，保证应用退到后台时麦克风与播放不被系统回收。
 */
class VoiceForegroundService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRequestListener?.invoke()
                stopForegroundAndSelf()
            }

            ACTION_START_PLAYBACK -> startWithType(
                type = playbackType(),
                title = getString(R.string.voice_notification_playback_title),
            )

            else -> startWithType(
                type = recordingType(),
                title = getString(R.string.voice_notification_recording_title),
            )
        }
        return START_NOT_STICKY
    }

    private fun playbackType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }

    private fun recordingType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        } else {
            0
        }

    private fun startWithType(type: Int, title: String) {
        createChannel()
        val notification = buildNotification(
            title = title,
            content = getString(R.string.voice_notification_running_text),
        )
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, VoiceForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, getString(R.string.voice_notification_stop), stopIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.voice_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.voice_notification_channel_description)
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun stopForegroundAndSelf() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ID = "voice_foreground"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START_RECORDING =
            "com.example.scenicguide.action.START_RECORDING"
        private const val ACTION_START_PLAYBACK =
            "com.example.scenicguide.action.START_PLAYBACK"
        private const val ACTION_STOP = "com.example.scenicguide.action.STOP"

        /** 通知栏“停止”按钮触发时通知页面结束录音或播报，页面销毁时需置空。 */
        var stopRequestListener: (() -> Unit)? = null

        fun startForRecording(context: Context) {
            start(context, ACTION_START_RECORDING)
        }

        fun startForPlayback(context: Context) {
            start(context, ACTION_START_PLAYBACK)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceForegroundService::class.java))
        }

        private fun start(context: Context, action: String) {
            val intent = Intent(context, VoiceForegroundService::class.java).setAction(action)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

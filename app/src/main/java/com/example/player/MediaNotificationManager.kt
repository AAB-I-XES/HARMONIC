package com.example.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.TrackEntity

class MediaNotificationManager(
    private val context: Context,
    private val audioPlayer: AudioPlayer
) {
    private val TAG = "MediaNotification"
    private val CHANNEL_ID = "harmonic_playback_channel"
    private val NOTIFICATION_ID = 4040
    
    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.harmonic.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.harmonic.NEXT"
        const val ACTION_PREV = "com.example.harmonic.PREV"
    }

    private var isReceiverRegistered = false

    private val playbackReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                Log.d(TAG, "Notification Intent received: $action")
                when (action) {
                    ACTION_PLAY_PAUSE -> audioPlayer.togglePlayPause()
                    ACTION_NEXT -> audioPlayer.next()
                    ACTION_PREV -> audioPlayer.previous()
                }
                // Refresh the active notification with the current track state
                val currentTrack = audioPlayer.currentTrack.value
                val isPlaying = audioPlayer.isPlaying.value
                if (currentTrack != null) {
                    showNotification(currentTrack, isPlaying)
                }
            }
        }
    }

    init {
        createNotificationChannel()
        registerReceiver()
    }

    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Playback Controls",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Shows active playback controls for Harmonic Music Player"
                    setShowBadge(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification channel: ${e.message}")
        }
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY_PAUSE)
                addAction(ACTION_NEXT)
                addAction(ACTION_PREV)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        playbackReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    ContextCompat.registerReceiver(
                        context,
                        playbackReceiver,
                        filter,
                        ContextCompat.RECEIVER_NOT_EXPORTED
                    )
                }
                isReceiverRegistered = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register broadcast receiver with receiver flags: ${e.message}. Trying legacy fallback.")
                try {
                    context.registerReceiver(playbackReceiver, filter)
                    isReceiverRegistered = true
                } catch (ex: Exception) {
                    Log.e(TAG, "Legacy register receiver registration also failed: ${ex.message}")
                }
            }
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(playbackReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver: ${e.message}")
            }
            isReceiverRegistered = false
        }
    }

    fun showNotification(track: TrackEntity, isPlaying: Boolean) {
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val clickPendingIntent = PendingIntent.getActivity(
            context, 0, clickIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous button action
        val prevIntent = Intent(ACTION_PREV).apply {
            setPackage(context.packageName)
        }
        val prevPendingIntent = PendingIntent.getBroadcast(
            context, 1, prevIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Play/Pause button action
        val playPauseIntent = Intent(ACTION_PLAY_PAUSE).apply {
            setPackage(context.packageName)
        }
        val playPausePendingIntent = PendingIntent.getBroadcast(
            context, 2, playPauseIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Next button action
        val nextIntent = Intent(ACTION_NEXT).apply {
            setPackage(context.packageName)
        }
        val nextPendingIntent = PendingIntent.getBroadcast(
            context, 3, nextIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(track.title)
            .setContentText(track.artistName)
            .setSubText(track.albumName)
            .setContentIntent(clickPendingIntent)
            .setOngoing(isPlaying)
            .setAutoCancel(!isPlaying)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Add media controls actions
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, if (isPlaying) "Pause" else "Play", playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            // Modern Big Media Notification styling (Standard Media Style fallback)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}

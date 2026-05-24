package com.example.walkmanapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.example.walkmanapp.models.Song
import com.example.walkmanapp.models.PlaybackMode
import android.app.Notification
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.walkmanapp.R

class MusicService : Service() {

    private val binder = MusicBinder()

    private val CHANNEL_ID = "walkman_playback"

    var mediaPlayer: MediaPlayer? = null
        private set

    var currentSong: Song? = null
        private set

    var isPlaying = false
        private set

    var playbackMode = PlaybackMode.OFF

    var isShuffleEnabled = false

    var currentSongIndex = 0

    var onNextRequested: (() -> Unit)? = null

    var onPreviousRequested: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun loadSong(song: Song) {

        currentSong = song

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {

            setDataSource(song.path)

            prepare()
        }

        refreshNotification()
    }

    fun play() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
            isPlaying = true
            startForeground(
                1,
                buildNotification()
            )
        }

        refreshNotification()
    }

    fun pause() {

        mediaPlayer?.let {

            if (it.isPlaying) {
                it.pause()
            }

            isPlaying = false

            val notificationManager =
                getSystemService(NotificationManager::class.java)

            notificationManager.notify(
                1,
                buildNotification()
            )
        }

        refreshNotification()
    }

    fun seekTo(position: Int) {

        mediaPlayer?.seekTo(position)
    }

    fun getCurrentPosition(): Int {

        return mediaPlayer?.currentPosition ?: 0
    }

    fun getDuration(): Int {

        return mediaPlayer?.duration ?: 0
    }

    fun releasePlayer() {

        mediaPlayer?.release()

        mediaPlayer = null

        isPlaying = false
    }

    private fun buildNotification(): Notification {

        val playPauseIntent =
            Intent(this, MusicService::class.java).apply {

                action = ACTION_PLAY_PAUSE
            }

        val nextIntent =
            Intent(this, MusicService::class.java).apply {

                action = ACTION_NEXT
            }

        val previousIntent =
            Intent(this, MusicService::class.java).apply {

                action = ACTION_PREVIOUS
            }

        val playPausePendingIntent =
            PendingIntent.getService(
                this,
                0,
                playPauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val nextPendingIntent =
            PendingIntent.getService(
                this,
                1,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val previousPendingIntent =
            PendingIntent.getService(
                this,
                2,
                previousIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note)

            .setContentTitle(
                currentSong?.title ?: "Walkman"
            )

            .setContentText(
                if (isPlaying)
                    "Reproduciendo música"
                else
                    "Música en pausa"
            )

            .setOngoing(isPlaying)

            .addAction(
                R.drawable.ic_skip_previous,
                "Anterior",
                previousPendingIntent
            )

            .addAction(
                if (isPlaying)
                    R.drawable.ic_pause
                else
                    R.drawable.ic_play,
                if (isPlaying)
                    "Pausa"
                else
                    "Play",

                playPausePendingIntent
            )

            .addAction(
                R.drawable.ic_skip_next,
                "Siguiente",
                nextPendingIntent
            )

            .setStyle(
                MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )

            .build()
    }

    private fun refreshNotification() {

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.notify(
            1,
            buildNotification()
        )
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Walkman Playback",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(channel)
        }
    }

    companion object {

        const val ACTION_PLAY_PAUSE =
            "ACTION_PLAY_PAUSE"

        const val ACTION_NEXT =
            "ACTION_NEXT"

        const val ACTION_PREVIOUS =
            "ACTION_PREVIOUS"
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        when(intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (isPlaying) {
                    pause()
                } else {
                    play()
                }
            }

            ACTION_NEXT -> {
                onNextRequested?.invoke()
            }
            ACTION_PREVIOUS -> {
                onPreviousRequested?.invoke()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {

        super.onDestroy()

        releasePlayer()
    }
}
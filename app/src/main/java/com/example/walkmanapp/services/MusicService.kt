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
import androidx.core.app.NotificationCompat
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

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentSong?.title ?: "Walkman")
            .setContentText(
                if (isPlaying)
                    "Reproduciendo música"
                else
                    "Música en pausa"
            )
            .setSmallIcon(R.drawable.ic_note)
            .setOngoing(isPlaying)
            .build()
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

    override fun onDestroy() {

        super.onDestroy()

        releasePlayer()
    }
}
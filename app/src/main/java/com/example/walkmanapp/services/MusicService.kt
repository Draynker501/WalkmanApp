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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Handler
import android.os.Looper
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.walkmanapp.R
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.RemoteViews
import androidx.media.session.MediaButtonReceiver

class MusicService : Service() {

    private val binder = MusicBinder()

    private val CHANNEL_ID = "walkman_playback"

    private lateinit var mediaSession: MediaSessionCompat

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

        mediaSession = MediaSessionCompat(this, "WalkmanSession")

        mediaSession.setCallback(
            object : MediaSessionCompat.Callback() {

                override fun onPlay() {
                    play()
                }

                override fun onPause() {
                    pause()
                }

                override fun onSkipToNext() {
                    onNextRequested?.invoke()
                }

                override fun onSkipToPrevious() {
                    onPreviousRequested?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos.toInt())
                }
            }
        )

        mediaSession.isActive = true
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

        updateMediaSession()
        refreshNotification()
    }

    fun play() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
            }
            isPlaying = true
            updateMediaSession()
            startForeground(
                1,
                buildNotification()
            )
        }

        handler.post(notificationRunnable)

        refreshNotification()
    }

    fun pause() {

        mediaPlayer?.let {

            if (it.isPlaying) {
                it.pause()
            }
            isPlaying = false
            updateMediaSession()
            val notificationManager =
                getSystemService(NotificationManager::class.java)

            notificationManager.notify(
                1,
                buildNotification()
            )
        }

        handler.removeCallbacks(notificationRunnable)

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

    private fun updateMediaSession() {
        val state =
            if (isPlaying)
                PlaybackStateCompat.STATE_PLAYING
            else
                PlaybackStateCompat.STATE_PAUSED

        val artwork = getArtworkOrDefault()

        val playbackState =
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
                .setState(
                    state,
                    getCurrentPosition().toLong(),
                    1f
                )
                .build()

        mediaSession.setPlaybackState(playbackState)
        val metadata =
            MediaMetadataCompat.Builder()
                .putString(
                    MediaMetadataCompat.METADATA_KEY_TITLE,
                    currentSong?.title ?: "Unknown"
                )
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ARTIST,
                    currentSong?.artist ?: "Unknown artist"
                )
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    getDuration().toLong()
                )
                .putBitmap(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART,
                    artwork
                )
                .build()

        mediaSession.setMetadata(metadata)
    }

    private fun getAlbumArt(path: String): Bitmap? {

        return try {

            val retriever = MediaMetadataRetriever()

            retriever.setDataSource(path)

            val artBytes = retriever.embeddedPicture

            if (artBytes != null) {

                BitmapFactory.decodeByteArray(
                    artBytes,
                    0,
                    artBytes.size
                )

            } else {
                null
            }

        } catch (e: Exception) {
            null
        }
    }

    private fun getArtworkOrDefault(): Bitmap {

        val artwork =
            currentSong?.path?.let {
                getAlbumArt(it)
            }

        return artwork
            ?: BitmapFactory.decodeResource(
                resources,
                R.drawable.walkman_background
            )
    }

    private fun buildNotification(): Notification {

        val currentPosition =
            mediaPlayer?.currentPosition ?: 0

        val duration =
            mediaPlayer?.duration ?: 0

        val expandedView =
            RemoteViews(
                packageName,
                R.layout.notification_expanded
            )
        expandedView.setTextViewText(
            R.id.txtSong,
            currentSong?.title ?: "Unknown"
        )
        expandedView.setImageViewResource(
            R.id.btnPlay,
            if (isPlaying)
                R.drawable.ic_pause
            else
                R.drawable.ic_play
        )
        expandedView.setTextViewText(
            R.id.txtArtist,
            "Unknown Artist"
        )
        expandedView.setProgressBar(
            R.id.progressBar,
            duration,
            currentPosition,
            false
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_note)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)

            .addAction(
                R.drawable.ic_skip_previous,
                "Previous",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                )
            )

            .addAction(
                if (isPlaying)
                    R.drawable.ic_pause
                else
                    R.drawable.ic_play,
                "Play",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    if (isPlaying)
                        PlaybackStateCompat.ACTION_PAUSE
                    else
                        PlaybackStateCompat.ACTION_PLAY
                )
            )

            .addAction(
                R.drawable.ic_skip_next,
                "Next",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    this,
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
            )

            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
            )

            .build()
    }



    private val handler = Handler(Looper.getMainLooper())

    private val notificationRunnable =
        object : Runnable {

            override fun run() {

                if (isPlaying) {

                    updatePlaybackState()

                    refreshNotification()

                    handler.postDelayed(this, 1000)
                }
            }
        }

    private fun updatePlaybackState() {

        val state =
            if (isPlaying)
                PlaybackStateCompat.STATE_PLAYING
            else
                PlaybackStateCompat.STATE_PAUSED

        val playbackState =
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(
                    state,
                    getCurrentPosition().toLong(),
                    1f
                )
                .build()

        mediaSession.setPlaybackState(playbackState)
    }

    private fun refreshNotification() {

        val manager =
            getSystemService(NotificationManager::class.java)

        manager.notify(
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

    private fun formatTime(ms: Int): String {

        val totalSeconds = ms / 1000

        val minutes = totalSeconds / 60

        val seconds = totalSeconds % 60

        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onDestroy() {

        super.onDestroy()

        handler.removeCallbacks(notificationRunnable)

        releasePlayer()
    }
}
package com.example.walkmanapp.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import com.example.walkmanapp.models.Song
import com.example.walkmanapp.models.PlaybackMode

class MusicService : Service() {

    private val binder = MusicBinder()

    var mediaPlayer: MediaPlayer? = null
        private set

    var currentSong: Song? = null
        private set

    var isPlaying = false
        private set

    var playbackMode = PlaybackMode.OFF

    var isShuffleEnabled = false

    var currentSongIndex = 0

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

        mediaPlayer?.start()

        isPlaying = true
    }

    fun pause() {

        mediaPlayer?.pause()

        isPlaying = false
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

    override fun onDestroy() {

        super.onDestroy()

        releasePlayer()
    }
}
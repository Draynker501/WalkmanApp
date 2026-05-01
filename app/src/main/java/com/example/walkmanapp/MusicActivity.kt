package com.example.walkmanapp

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MusicActivity : AppCompatActivity() {

    private lateinit var btnPlayMusic: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var btnModeRecord: Button

    private lateinit var seekBar: SeekBar
    private lateinit var txtTitle: TextView

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    private var animLeft: ObjectAnimator? = null
    private var animRight: ObjectAnimator? = null

    private lateinit var txtCurrentTime: TextView
    private lateinit var txtDuration: TextView

    private lateinit var cassetteView: CassetteView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        btnPlayMusic = findViewById(R.id.btnPlayMusic)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnModeRecord = findViewById(R.id.btnModeRecord)

        seekBar = findViewById(R.id.seekBar)
        txtTitle = findViewById(R.id.txtTitle)

        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtDuration = findViewById(R.id.txtDuration)

        cassetteView = findViewById(R.id.cassetteView)

        initPlayer()

        btnPlayMusic.setOnClickListener {
            if (isPlaying) pauseMusic() else playMusic()
        }

        btnBack.setOnClickListener {
            mediaPlayer?.seekTo((mediaPlayer!!.currentPosition - 5000).coerceAtLeast(0))
        }

        btnForward.setOnClickListener {
            mediaPlayer?.seekTo(mediaPlayer!!.currentPosition + 5000)
        }

        btnModeRecord.setOnClickListener {
            startActivity(Intent(this, RecordActivity::class.java))
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    private fun initPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.mm_intro)
        txtTitle.text = "mm_intro.mp3"

        txtDuration.text = formatTime(mediaPlayer?.duration ?: 0)
        txtCurrentTime.text = "00:00"

        mediaPlayer?.setOnCompletionListener {
            isPlaying = false
            cassetteView.setProgress(0f)
            btnPlayMusic.setImageResource(android.R.drawable.ic_media_play)

            stopSeekBar()
            stopReels()

            seekBar.progress = 0
            txtCurrentTime.text = "00:00"
        }
    }

    private fun playMusic() {
        mediaPlayer?.start()
        isPlaying = true
        btnPlayMusic.setImageResource(android.R.drawable.ic_media_pause)
        startSeekBar()
        startReels()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayMusic.setImageResource(android.R.drawable.ic_media_play)
        stopReels()
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        return String.format("%02d:%02d", sec / 60, sec % 60)
    }

    private fun startSeekBar() {
        val player = mediaPlayer ?: return

        seekBar.max = player.duration

        // mostrar duración total
        txtDuration.text = formatTime(player.duration)

        updateRunnable = object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    val current = player.currentPosition
                    val progress = current.toFloat() / player.duration
                    cassetteView.setProgress(progress)
                    cassetteView.updateRotation()

                    seekBar.progress = current
                    txtCurrentTime.text = formatTime(current)

                    handler.postDelayed(this, 16)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopSeekBar() {
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun startReels() {
        animLeft?.start()
        animRight?.start()
    }

    private fun stopReels() {
        animLeft?.cancel()
        animRight?.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        stopSeekBar()
    }
}
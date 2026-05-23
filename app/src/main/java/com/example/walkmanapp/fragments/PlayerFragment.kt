package com.example.walkmanapp.fragments

import android.os.Bundle
import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.walkmanapp.views.CassetteView
import com.example.walkmanapp.R
import com.example.walkmanapp.activities.RecordActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.lifecycle.ViewModelProvider
import com.example.walkmanapp.models.Song
import com.example.walkmanapp.viewmodels.MusicViewModel

class PlayerFragment : Fragment() {
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
    private lateinit var btnModeMusic: Button
    private var lastProgress = 0

    private var pendingSeek = 0

    private lateinit var musicViewModel: MusicViewModel

    private var currentSong: Song? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_player,
            container,
            false
        )

        btnPlayMusic = view.findViewById(R.id.btnPlayMusic)
        btnBack = view.findViewById(R.id.btnBack)
        btnForward = view.findViewById(R.id.btnForward)
        btnModeRecord = view.findViewById(R.id.btnModeRecord)

        seekBar = view.findViewById(R.id.seekBar)
        txtTitle = view.findViewById(R.id.txtTitle)

        txtCurrentTime = view.findViewById(R.id.txtCurrentTime)
        txtDuration = view.findViewById(R.id.txtDuration)

        cassetteView = view.findViewById(R.id.cassetteView)

        btnModeMusic = view.findViewById(R.id.btnModeMusic)

        musicViewModel =
            ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        observeSong()

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
            startActivity(Intent(requireContext(), RecordActivity::class.java))
        }

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {

                    pendingSeek = progress

                    if (isPlaying) {
                        mediaPlayer?.seekTo(progress)
                    }

                    txtCurrentTime.text = formatTime(progress)

                    val duration = mediaPlayer?.duration ?: 1
                    val progressFloat = progress.toFloat() / duration

                    cassetteView.setProgress(progressFloat)

                    val direction = when {
                        progress > lastProgress -> 1
                        progress < lastProgress -> -1
                        else -> 0
                    }

                    cassetteView.updateRotation(direction)

                    lastProgress = progress
                }
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Simular que ESTE está presionado (porque estás en MusicActivity)
        btnModeMusic.post {
            btnModeMusic.animate()
                .translationX(-25f) // se mete hacia adentro
                .setDuration(120)
                .start()

            val params = btnModeMusic.layoutParams
            params.width = dpToPx(28)
            btnModeMusic.layoutParams = params
        }

        btnModeMusic.isSelected = true

        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottomNavigation)

        return view
    }

    private fun observeSong() {

        musicViewModel.selectedSong.observe(viewLifecycleOwner) { song ->

            currentSong = song

            loadSelectedSong(song)
        }
    }

    private fun loadSelectedSong(song: Song) {

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {

            setDataSource(song.path)

            prepare()
        }

        txtTitle.text = song.title

        seekBar.max = mediaPlayer?.duration ?: 0

        txtDuration.text =
            formatTime(mediaPlayer?.duration ?: 0)

        txtCurrentTime.text = "00:00"

        pendingSeek = 0

        playMusic()
    }

    private fun playMusic() {

        mediaPlayer?.seekTo(pendingSeek)

        mediaPlayer?.start()

        isPlaying = true

        btnPlayMusic.setImageResource(android.R.drawable.ic_media_pause)

        stopSeekBar()
        startSeekBar()

        startReels()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        btnPlayMusic.setImageResource(android.R.drawable.ic_media_play)
        stopSeekBar()
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
                }
                handler.postDelayed(this, 16)
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

    override fun onDestroyView() {
        super.onDestroy()
        mediaPlayer?.release()
        stopSeekBar()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}
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
import androidx.lifecycle.ViewModelProvider
import com.example.walkmanapp.models.PlaybackMode
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

    private var playbackMode = PlaybackMode.OFF

    private lateinit var btnPlaybackMode: ImageButton

    private lateinit var btnShuffle: ImageButton

    private var isShuffleEnabled = false

    private val playbackHistory = mutableListOf<Int>()

    private val shuffleQueue = mutableListOf<Int>()

    private var isManualSelection = false

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
            handlePreviousSong()
        }

        btnForward.setOnClickListener {
            if (isShuffleEnabled) {
                playRandomSong(true)
            } else {
                playNextSong(true)
            }
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

        btnPlaybackMode =
            view.findViewById(R.id.btnPlaybackMode)

        btnShuffle =
            view.findViewById(R.id.btnShuffle)

        btnShuffle.setOnClickListener {

            isShuffleEnabled = !isShuffleEnabled

            if (isShuffleEnabled) {
                refillShuffleQueue()
            } else {
                shuffleQueue.clear()
            }

            updateShuffleUI()
        }

        btnPlaybackMode.setOnClickListener {
            playbackMode = when(playbackMode) {
                PlaybackMode.OFF -> {
                    PlaybackMode.REPEAT_ALL
                }
                PlaybackMode.REPEAT_ALL -> {
                    PlaybackMode.REPEAT_ONE
                }
                PlaybackMode.REPEAT_ONE -> {
                    PlaybackMode.STOP_AFTER
                }
                PlaybackMode.STOP_AFTER -> {
                    PlaybackMode.OFF
                }
            }

            updatePlaybackModeUI()
            updateShuffleUI()
        }

        return view
    }

    private fun observeSong() {

        musicViewModel.selectedSong.observe(viewLifecycleOwner) { song ->
            currentSong = song
            // selección manual: reiniciar shuffle

            if (isManualSelection) {
                if (isShuffleEnabled) {
                    refillShuffleQueue()
                }
                isManualSelection = false
            }
            loadSelectedSong(song)
        }
    }

    private fun loadSelectedSong(song: Song) {

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {

            setDataSource(song.path)

            prepare()
        }

        updatePlayerUI()

        pendingSeek = 0

        mediaPlayer?.setOnCompletionListener {

            handleSongCompletion()
        }

        playMusic()
    }

    private fun playNextSong(isManualSkip: Boolean = false) {

        val songs =
            musicViewModel.songsList.value ?: return

        if (songs.isEmpty()) return

        addToHistory(
            musicViewModel.currentIndex
        )

        musicViewModel.currentIndex++

        if (musicViewModel.currentIndex >= songs.size) {

            musicViewModel.currentIndex = 0
        }

        val nextSong =
            songs[musicViewModel.currentIndex]

        currentSong = nextSong

        loadSelectedSong(nextSong)
    }

    private fun handlePreviousSong() {

        val player = mediaPlayer ?: return

        // Si lleva más de 3 segundos: reiniciar canción actual


        if (player.currentPosition > 3000) {

            player.seekTo(0)

            return
        }

        // SHUFFLE: volver a canciones reproducidas


        if (isShuffleEnabled) {

            if (playbackHistory.isEmpty()) return

            val previousIndex =
                playbackHistory.removeAt(
                    playbackHistory.lastIndex
                )

            musicViewModel.currentIndex =
                previousIndex

            val songs =
                musicViewModel.songsList.value ?: return

            val previousSong =
                songs[previousIndex]

            currentSong = previousSong

            loadSelectedSong(previousSong)

            return
        }

        //NORMAL: canción anterior por índice

        val songs =
            musicViewModel.songsList.value ?: return

        if (songs.isEmpty()) return

        // Si estamos en la primera canción


        if (musicViewModel.currentIndex == 0) {

            val loopEnabled =
                playbackMode == PlaybackMode.REPEAT_ALL ||
                        playbackMode == PlaybackMode.REPEAT_ONE

            // sin loop: quedarse en primera canción


            if (!loopEnabled) {

                player.seekTo(0)

                return
            }

            // ir a la última


            musicViewModel.currentIndex =
                songs.lastIndex

        } else {

            musicViewModel.currentIndex--
        }

        val previousSong =
            songs[musicViewModel.currentIndex]

        currentSong = previousSong

        loadSelectedSong(previousSong)
    }

    private fun handleSongCompletion() {
        isPlaying = false

        btnPlayMusic.setImageResource(
            android.R.drawable.ic_media_play
        )

        when(playbackMode) {
            PlaybackMode.OFF -> {
                if (isShuffleEnabled) {
                    playRandomSong()
                } else {
                    playNextSongWithoutLoop()
                }
            }
            PlaybackMode.REPEAT_ALL -> {
                if (isShuffleEnabled) {
                    playRandomSong()
                } else {
                    playNextSong()
                }
            }
            PlaybackMode.REPEAT_ONE -> {
                currentSong?.let {
                    loadSelectedSong(it)
                }
            }
            PlaybackMode.STOP_AFTER -> {
                prepareNextSongPaused()
            }
        }
    }

    private fun playRandomSong(isManualSkip: Boolean = false) {

        val songs =
            musicViewModel.songsList.value ?: return

        if (songs.isEmpty()) return

        // si ya no quedan canciones

        if (shuffleQueue.isEmpty()) {

            when(playbackMode) {

                PlaybackMode.OFF -> {

                    if (isManualSkip) {
                        refillShuffleQueue()
                    } else {
                        prepareFirstShuffleSongPaused()

                        return
                    }
                }

                PlaybackMode.REPEAT_ALL -> {
                    refillShuffleQueue()
                }
                else -> {
                    refillShuffleQueue()
                }
            }
        }

        // sacar siguiente canción random

        val randomIndex =
            shuffleQueue.removeAt(0)

        addToHistory(
            musicViewModel.currentIndex
        )

        musicViewModel.currentIndex =
            randomIndex

        val randomSong =
            songs[randomIndex]

        currentSong = randomSong

        loadSelectedSong(randomSong)
    }

    private fun prepareFirstShuffleSongPaused() {

        val songs =
            musicViewModel.songsList.value ?: return

        if (songs.isEmpty()) return

        // crear nueva cola shuffle

        refillShuffleQueue()

        // tomar PRIMER canción de la cola


        val nextIndex =
            shuffleQueue.first()

        musicViewModel.currentIndex =
            nextIndex

        val nextSong =
            songs[nextIndex]

        currentSong = nextSong

        mediaPlayer?.release()

        mediaPlayer = MediaPlayer().apply {

            setDataSource(nextSong.path)

            prepare()
        }

        updatePlayerUI()

        pendingSeek = 0

        cassetteView.setProgress(0f)

        cassetteView.invalidate()

        isPlaying = false

        btnPlayMusic.setImageResource(
            android.R.drawable.ic_media_play
        )

        stopSeekBar()

        stopReels()
    }

    private fun addToHistory(index: Int) {

        playbackHistory.add(index)
    }

    private fun playNextSongWithoutLoop(isManualSkip: Boolean = false) {
        val songs =
            musicViewModel.songsList.value ?: return
        if (songs.isEmpty()) return
        if (musicViewModel.currentIndex >= songs.lastIndex) {

            if (isManualSkip) {

                musicViewModel.currentIndex = 0

                val firstSong =
                    songs[musicViewModel.currentIndex]

                currentSong = firstSong

                loadSelectedSong(firstSong)

                return
            }

            // automático: preparar primera canción pausada

            musicViewModel.currentIndex = 0

            val firstSong =
                songs[musicViewModel.currentIndex]

            currentSong = firstSong
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {

                setDataSource(firstSong.path)

                prepare()
            }

            updatePlayerUI()

            pendingSeek = 0
            cassetteView.setProgress(0f)
            cassetteView.invalidate()
            isPlaying = false

            stopSeekBar()

            stopReels()

            return
        }

        addToHistory(
            musicViewModel.currentIndex
        )
        musicViewModel.currentIndex++
        val nextSong =
            songs[musicViewModel.currentIndex]
        currentSong = nextSong
        loadSelectedSong(nextSong)
    }

    private fun prepareNextSongPaused() {
        val songs =
            musicViewModel.songsList.value ?: return
        if (songs.isEmpty()) return
        addToHistory(
            musicViewModel.currentIndex
        )
        musicViewModel.currentIndex++
        if (musicViewModel.currentIndex >= songs.size) {
            musicViewModel.currentIndex = 0
        }
        val nextSong =
            songs[musicViewModel.currentIndex]
        currentSong = nextSong
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(nextSong.path)
            prepare()
        }
        updatePlayerUI()
        pendingSeek = 0
        cassetteView.setProgress(0f)
        cassetteView.invalidate()
        isPlaying = false
        btnPlayMusic.setImageResource(
            android.R.drawable.ic_media_play
        )
        stopSeekBar()
        stopReels()
    }

    private fun refillShuffleQueue() {
        val songs =
            musicViewModel.songsList.value ?: return

        shuffleQueue.clear()
        shuffleQueue.addAll(
            songs.indices.shuffled()
        )

        // evitar que la canción actual salga inmediatamente otra vez

        shuffleQueue.remove(
            musicViewModel.currentIndex
        )
    }

    private fun playMusic() {

        mediaPlayer?.start()

        isPlaying = true

        updatePlayerUI()

        stopSeekBar()
        startSeekBar()

        startReels()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlaying = false
        updatePlayerUI()
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
        super.onDestroyView()
        mediaPlayer?.release()
        stopSeekBar()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updatePlayerUI() {
        val player = mediaPlayer ?: return
        txtTitle.text =
            currentSong?.title ?: "NO TAPE"
        seekBar.max = player.duration
        txtDuration.text =
            formatTime(player.duration)
        txtCurrentTime.text =
            formatTime(player.currentPosition)
        seekBar.progress =
            player.currentPosition
        if (isPlaying) {
            btnPlayMusic.setImageResource(
                android.R.drawable.ic_media_pause
            )
        } else {
            btnPlayMusic.setImageResource(
                android.R.drawable.ic_media_play
            )
        }
    }

    private fun updatePlaybackModeUI() {
        when(playbackMode) {PlaybackMode.OFF -> {
                btnPlaybackMode.setImageResource(
                    R.drawable.ic_drepeat
                )
            btnPlaybackMode.setColorFilter(
                android.graphics.Color.GRAY
            )
            btnPlaybackMode.scaleX = 1f
            btnPlaybackMode.scaleY = 1f
            }
            PlaybackMode.REPEAT_ALL -> {
                btnPlaybackMode.setImageResource(
                    R.drawable.ic_repeat
                )
                btnPlaybackMode.setColorFilter(
                    android.graphics.Color.WHITE
                )
                btnPlaybackMode.scaleX = 1.15f
                btnPlaybackMode.scaleY = 1.15f
            }
            PlaybackMode.REPEAT_ONE -> {
                btnPlaybackMode.setImageResource(
                    R.drawable.ic_repeatb
                )
                btnPlaybackMode.setColorFilter(
                    android.graphics.Color.WHITE
                )
                btnPlaybackMode.scaleX = 1.15f
                btnPlaybackMode.scaleY = 1.15f
            }
            PlaybackMode.STOP_AFTER -> {
                btnPlaybackMode.setImageResource(
                    R.drawable.ic_endqueue
                )
                btnPlaybackMode.setColorFilter(
                    android.graphics.Color.WHITE
                )
                btnPlaybackMode.scaleX = 1.15f
                btnPlaybackMode.scaleY = 1.15f
            }
        }
    }

    private fun updateShuffleUI() {
        if (isShuffleEnabled) {
            btnShuffle.setColorFilter(
                android.graphics.Color.WHITE
            )
            btnShuffle.scaleX = 1.15f
            btnShuffle.scaleY = 1.15f
        } else {
            btnShuffle.setColorFilter(
                android.graphics.Color.GRAY
            )
            btnShuffle.scaleX = 1f
            btnShuffle.scaleY = 1f
        }
    }
}
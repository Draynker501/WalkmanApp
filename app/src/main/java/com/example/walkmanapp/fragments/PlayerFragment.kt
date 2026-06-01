    package com.example.walkmanapp.fragments

    import android.os.Bundle
    import android.animation.ObjectAnimator
    import android.content.Intent
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
    import android.content.ComponentName
    import android.content.Context
    import android.content.ServiceConnection
    import android.os.IBinder
    import com.example.walkmanapp.services.MusicService

    class PlayerFragment : Fragment() {
        private lateinit var btnPlayMusic: ImageButton

        private lateinit var btnBack: ImageButton

        private lateinit var btnForward: ImageButton

        private lateinit var btnModeRecord: Button

        private lateinit var seekBar: SeekBar

        private lateinit var txtTitle: TextView

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

        private lateinit var btnPlaybackMode: ImageButton

        private lateinit var btnShuffle: ImageButton

        private var isManualSelection = false

        private var musicService: MusicService? = null

        private var isBound = false

        private var cachedDuration = 0

        private var isUserSeeking = false

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

            btnPlayMusic.setOnClickListener {
                if (musicService?.isPlaying == true) pauseMusic() else playMusic()
            }

            btnBack.setOnClickListener {
                musicService?.playPreviousSong()
            }

            btnForward.setOnClickListener {
                musicService?.playNextSong(true)
            }

            btnModeRecord.setOnClickListener {
                startActivity(Intent(requireContext(), RecordActivity::class.java))
            }

            seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {

                        pendingSeek = progress

                        txtCurrentTime.text = formatTime(progress)

                        val duration = musicService?.mediaPlayer?.duration ?: 1
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
                override fun onStartTrackingTouch(sb: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(sb: SeekBar?) {
                    isUserSeeking = false
                    musicService?.seekTo(pendingSeek)
                }
            })

            // Simular que el botón lateral está presionado (porque estás en PlayerFragment o MainActivity)
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

                val enabled =
                    musicService?.isShuffleEnabled ?: false

                musicService?.isShuffleEnabled = !enabled

                updateShuffleUI()
            }

            btnPlaybackMode.setOnClickListener {
                musicService?.playbackMode =
                    when(musicService?.playbackMode) {
                        PlaybackMode.OFF ->
                            PlaybackMode.REPEAT_ALL
                        PlaybackMode.REPEAT_ALL ->
                            PlaybackMode.REPEAT_ONE
                        PlaybackMode.REPEAT_ONE ->
                            PlaybackMode.STOP_AFTER
                        PlaybackMode.STOP_AFTER,
                        null ->
                            PlaybackMode.OFF
                    }

                updatePlaybackModeUI()
            }

            val intent =
                Intent(requireContext(), MusicService::class.java)

            requireContext().startService(intent)

            requireActivity().bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            return view
        }

        private val serviceConnection = object : ServiceConnection {

            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?
            ) {
                val binder =
                    service as MusicService.MusicBinder

                musicService =
                    binder.getService()

                musicService?.onSongCompleted = {

                    activity?.runOnUiThread {

                        if (!isAdded) return@runOnUiThread

                        updatePlayerUI()
                    }
                }

                musicService?.onSongChanged = {

                    activity?.runOnUiThread {

                        if (!isAdded) return@runOnUiThread

                        updatePlayerUI()
                    }
                }

                isBound = true

                observeSong()

                musicService?.currentSong?.let {
                    txtTitle.text = it.title
                }

                musicService?.onPlaybackStateChanged = callback@{

                    if (!isAdded) return@callback

                    requireActivity().runOnUiThread {

                        updatePlayerUI()

                        if (musicService?.isPlaying == true) {
                            startSeekBar()
                            startReels()
                        } else {
                            stopSeekBar()
                            stopReels()
                        }
                    }
                }

                updatePlayerUI()
                updatePlaybackModeUI()
                updateShuffleUI()

                if (musicService?.isPlaying == true) {
                    startSeekBar()
                    startReels()
                }

                musicService?.onNextRequested = {

                    activity?.runOnUiThread {

                        if (!isAdded) return@runOnUiThread

                        musicService?.playNextSong(true)
                    }
                }

                musicService?.onPreviousRequested = {

                    activity?.runOnUiThread {

                        if (!isAdded) return@runOnUiThread
                        musicService?.playPreviousSong()
                    }
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {

                musicService = null

                isBound = false
            }
        }

        private fun observeSong() {

            musicViewModel.selectedSong.removeObservers(viewLifecycleOwner)

            musicViewModel.selectedSong.observe(viewLifecycleOwner) { song ->

                if (song == null) return@observe

                val songs =
                    musicViewModel.songsList.value ?: return@observe

                val selectedIndex =
                    songs.indexOfFirst { it.path == song.path }

                if (selectedIndex != -1) {
                    musicViewModel.currentIndex = selectedIndex
                }

                if (musicService?.currentSong?.path == song.path) {

                    updatePlayerUI()

                    if (musicService?.isPlaying == true) {
                        startSeekBar()
                        startReels()
                    }

                    return@observe
                }

                isManualSelection = true

                loadSelectedSong(song)

                musicViewModel.selectedSong.value = null
            }
        }

        private fun loadSelectedSong(song: Song) {

            val songs =
                musicViewModel.songsList.value ?: return

            val selectedIndex =
                songs.indexOf(song)

            musicService?.setPlaylist(
                songs,
                selectedIndex
            )

            musicService?.loadSong(song)

            musicService?.play()
        }

        private fun playMusic() {
            musicService?.play()
            updatePlayerUI()
            startReels()
        }

        private fun pauseMusic() {
            musicService?.pause()
            updatePlayerUI()
            stopSeekBar()
            stopReels()
        }

        private fun formatTime(ms: Int): String {
            val sec = ms / 1000
            return String.format("%02d:%02d", sec / 60, sec % 60)
        }

        private fun startSeekBar() {

            stopSeekBar()

            updateRunnable = object : Runnable {

                override fun run() {
                    val player =
                        musicService?.mediaPlayer
                    // fragment destruido o player inválido
                    if (
                        !isAdded ||
                        view == null ||
                        player == null
                    ) {
                        return
                    }

                    try {

                        if (musicService?.isPlaying == true) {
                            val duration = cachedDuration
                            if (duration > 0) {
                                val current =
                                    player.currentPosition

                                val progress =
                                    current.toFloat() / duration
                                /*cassetteView.setProgress(progress)
                                cassetteView.updateRotation()*/
                                if (!isUserSeeking) {
                                    seekBar.progress = current
                                    txtCurrentTime.text =
                                        formatTime(current)

                                    // Actualizar el progreso de los reels solo cuando el usuario está moviendo la progress bar
                                    // Revisar si me gusta así o lo vevuelvo a como era antes
                                    cassetteView.setProgress(progress)
                                    cassetteView.updateRotation()
                                }
                            }
                        }
                        handler.postDelayed(this, 16)

                    } catch (_: IllegalStateException) {

                        // player destruido mientras corría el runnable
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

        override fun onDestroyView() {
            super.onDestroyView()
            stopSeekBar()
            stopReels()
            if (isBound) {
                requireActivity().unbindService(serviceConnection)
                isBound = false
            }
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        private fun updatePlayerUI() {
            val player =
                musicService?.mediaPlayer ?: return
            cachedDuration = player.duration
            txtTitle.text =
                musicService?.currentSong?.title ?: "NO TAPE"
            seekBar.max = player.duration
            txtDuration.text =
                formatTime(player.duration)
            txtCurrentTime.text =
                formatTime(player.currentPosition)
            seekBar.progress =
                player.currentPosition
            if (musicService?.isPlaying == true) {
                btnPlayMusic.setImageResource(
                    android.R.drawable.ic_media_pause
                )
                startSeekBar()

            } else {
                btnPlayMusic.setImageResource(
                    android.R.drawable.ic_media_play
                )
                stopSeekBar()
            }
            updatePlaybackModeUI()
            updateShuffleUI()
        }

        private fun updatePlaybackModeUI() {

            when (musicService?.playbackMode) {

                PlaybackMode.OFF -> {

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

                null -> {

                    btnPlaybackMode.setImageResource(
                        R.drawable.ic_drepeat
                    )

                    btnPlaybackMode.setColorFilter(
                        android.graphics.Color.GRAY
                    )

                    btnPlaybackMode.scaleX = 1f
                    btnPlaybackMode.scaleY = 1f
                }
            }
        }

        private fun updateShuffleUI() {
            if (musicService?.isShuffleEnabled == true) {
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
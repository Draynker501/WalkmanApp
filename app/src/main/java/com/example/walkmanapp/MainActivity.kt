package com.example.walkmanapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // 🎵 MUSIC PLAYER
    private lateinit var btnPlayMusic: ImageButton
    private lateinit var btnBack: ImageButton
    private lateinit var btnForward: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var txtCurrentTime: TextView
    private lateinit var txtDuration: TextView
    private lateinit var txtTitle: TextView

    // 🎤 RECORD
    private lateinit var btnRecord: ImageButton
    private lateinit var btnPlayRecord: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var switchReverse: Switch
    private lateinit var txtRecording: TextView

    private lateinit var wavFile: File
    private lateinit var reversedFile: File

    private var isRecording = false
    private var isRecordingThread = false
    private var audioRecord: AudioRecord? = null

    private var recordedPlayer: MediaPlayer? = null
    private var isPlayingRecorded = false

    private var mediaPlayer: MediaPlayer? = null
    private var isPlayingMusic = false

    private val handler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI
        btnPlayMusic = findViewById(R.id.btnPlayMusic)
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        seekBar = findViewById(R.id.seekBar)
        txtCurrentTime = findViewById(R.id.txtCurrentTime)
        txtDuration = findViewById(R.id.txtDuration)
        txtTitle = findViewById(R.id.txtTitle)

        btnRecord = findViewById(R.id.btnRecord)
        btnPlayRecord = findViewById(R.id.btnPlayRecord)
        btnSave = findViewById(R.id.btnSave)
        switchReverse = findViewById(R.id.swReverse)
        txtRecording = findViewById(R.id.txtRecording)

        wavFile = File(cacheDir, "audio.wav")
        reversedFile = File(cacheDir, "audio_reverse.wav")

        // 🔥 LIMPIAR SIEMPRE AL INICIAR
        clearTemporaryAudio()

        initMusicPlayer()

        // 🎵 CONTROLES MÚSICA
        btnPlayMusic.setOnClickListener {
            if (isPlayingMusic) pauseMusic() else playMusic()
        }

        btnBack.setOnClickListener {
            mediaPlayer?.seekTo((mediaPlayer!!.currentPosition - 5000).coerceAtLeast(0))
        }

        btnForward.setOnClickListener {
            mediaPlayer?.seekTo(mediaPlayer!!.currentPosition + 5000)
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // 🎤 PERMISOS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        btnRecord.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        btnPlayRecord.setOnClickListener {
            toggleRecordedAudio()
        }

        switchReverse.setOnCheckedChangeListener { _, isChecked ->
            recordedPlayer?.release()
            recordedPlayer = null
            isPlayingRecorded = false
            btnPlayRecord.setImageResource(android.R.drawable.ic_media_play)

            if (isChecked && wavFile.exists()) {
                reverseAudio()
            }
        }

        btnSave.setOnClickListener {
            saveAudio()
        }
    }

    // ================= MUSIC =================

    private fun initMusicPlayer() {
        mediaPlayer = MediaPlayer.create(this, R.raw.mm_intro)
        txtTitle.text = "mm_intro.mp3"

        mediaPlayer?.setOnCompletionListener {
            isPlayingMusic = false
            btnPlayMusic.setImageResource(android.R.drawable.ic_media_play)
            stopSeekBarUpdates()
        }
    }

    private fun playMusic() {
        mediaPlayer?.start()
        isPlayingMusic = true
        btnPlayMusic.setImageResource(android.R.drawable.ic_media_pause)
        startSeekBarUpdates()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        isPlayingMusic = false
        btnPlayMusic.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun startSeekBarUpdates() {
        val player = mediaPlayer ?: return

        seekBar.max = player.duration
        txtDuration.text = formatTime(player.duration)

        updateRunnable = object : Runnable {
            override fun run() {
                if (player.isPlaying) {
                    seekBar.progress = player.currentPosition
                    txtCurrentTime.text = formatTime(player.currentPosition)
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(updateRunnable!!)
    }

    private fun stopSeekBarUpdates() {
        updateRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        return String.format("%02d:%02d", sec / 60, sec % 60)
    }

    // ================= RECORD =================

    private fun clearTemporaryAudio() {
        if (wavFile.exists()) wavFile.delete()
        if (reversedFile.exists()) reversedFile.delete()

        recordedPlayer?.release()
        recordedPlayer = null
        isPlayingRecorded = false

        btnPlayRecord.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun startRecording() {

        // 🔥 limpiar antes de grabar nuevo
        clearTemporaryAudio()

        val bufferSize = AudioRecord.getMinBufferSize(
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            44100,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecordingThread = true
        isRecording = true

        btnPlayRecord.isEnabled = false
        btnSave.isEnabled = false
        switchReverse.isEnabled = false

        txtRecording.visibility = View.VISIBLE

        thread { writeWavFile(bufferSize) }
    }

    private fun stopRecording() {
        isRecordingThread = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        isRecording = false
        txtRecording.visibility = View.GONE

        btnPlayRecord.isEnabled = true
        btnSave.isEnabled = true
        switchReverse.isEnabled = true
    }

    private fun writeWavFile(bufferSize: Int) {
        val file = RandomAccessFile(wavFile, "rw")

        writeWavHeader(file)

        val buffer = ByteArray(bufferSize)
        var total = 0

        while (isRecordingThread) {
            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
            if (read > 0) {
                file.write(buffer, 0, read)
                total += read
            }
        }

        file.seek(4)
        file.writeInt(Integer.reverseBytes(total + 36))
        file.seek(40)
        file.writeInt(Integer.reverseBytes(total))
        file.close()
    }

    private fun writeWavHeader(file: RandomAccessFile) {
        file.writeBytes("RIFF")
        file.writeInt(0)
        file.writeBytes("WAVEfmt ")
        file.writeInt(Integer.reverseBytes(16))
        file.writeShort(java.lang.Short.reverseBytes(1).toInt())
        file.writeShort(java.lang.Short.reverseBytes(1).toInt())
        file.writeInt(Integer.reverseBytes(44100))
        file.writeInt(Integer.reverseBytes(44100 * 2))
        file.writeShort(java.lang.Short.reverseBytes(2).toInt())
        file.writeShort(java.lang.Short.reverseBytes(16).toInt())
        file.writeBytes("data")
        file.writeInt(0)
    }

    // ================= PLAY RECORD =================

    private fun toggleRecordedAudio() {

        val file = if (switchReverse.isChecked) reversedFile else wavFile

        if (!file.exists()) {
            Toast.makeText(this, "Primero graba un audio", Toast.LENGTH_SHORT).show()
            return
        }

        if (recordedPlayer == null) {
            recordedPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }
            isPlayingRecorded = true
            btnPlayRecord.setImageResource(android.R.drawable.ic_media_pause)

        } else {
            if (isPlayingRecorded) {
                recordedPlayer?.pause()
                isPlayingRecorded = false
                btnPlayRecord.setImageResource(android.R.drawable.ic_media_play)
            } else {
                recordedPlayer?.start()
                isPlayingRecorded = true
                btnPlayRecord.setImageResource(android.R.drawable.ic_media_pause)
            }
        }

        recordedPlayer?.setOnCompletionListener {
            btnPlayRecord.setImageResource(android.R.drawable.ic_media_play)
            recordedPlayer?.release()
            recordedPlayer = null
            isPlayingRecorded = false
        }
    }

    // ================= REVERSE =================

    private fun reverseAudio() {
        if (!wavFile.exists()) return

        val bytes = wavFile.readBytes()
        val header = bytes.copyOfRange(0, 44)
        val data = bytes.copyOfRange(44, bytes.size)

        val reversed = ByteArray(data.size)
        var i = 0

        while (i < data.size - 1) {
            val j = data.size - i - 2
            reversed[i] = data[j]
            reversed[i + 1] = data[j + 1]
            i += 2
        }

        FileOutputStream(reversedFile).use {
            it.write(header)
            it.write(reversed)
        }
    }

    // ================= SAVE =================

    private fun saveAudio() {

        val source = if (switchReverse.isChecked) reversedFile else wavFile
        if (!source.exists()) {
            Toast.makeText(this, "No hay audio", Toast.LENGTH_SHORT).show()
            return
        }

        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Guardar audio")
            .setView(input)
            .setPositiveButton("Guardar") { _, _ ->

                val name = input.text.toString().ifEmpty {
                    "audio_${System.currentTimeMillis()}"
                }

                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, "$name.wav")
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/wav")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC)
                }

                val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)

                uri?.let {
                    contentResolver.openOutputStream(it)?.use { out ->
                        FileInputStream(source).copyTo(out)
                    }
                    Toast.makeText(this, "Guardado en Música", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()

        recordedPlayer?.release()
        mediaPlayer?.release()
        stopSeekBarUpdates()
    }
}
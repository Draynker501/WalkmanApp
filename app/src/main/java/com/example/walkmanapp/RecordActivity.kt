package com.example.walkmanapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
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

class RecordActivity : AppCompatActivity() {

    private lateinit var btnRecord: ImageButton
    private lateinit var btnPlay: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnModeMusic: Button

    private lateinit var txtRecording: TextView

    private lateinit var wavFile: File

    private var isRecording = false
    private var isRecordingThread = false
    private var audioRecord: AudioRecord? = null

    private var player: MediaPlayer? = null
    private var isPlaying = false

    private lateinit var switchReverse: Switch
    private lateinit var reversedFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlayRecord)
        btnSave = findViewById(R.id.btnSave)
        btnModeMusic = findViewById(R.id.btnModeMusic)
        txtRecording = findViewById(R.id.txtRecording)

        wavFile = File(cacheDir, "audio.wav")

        clearAudio()

        btnModeMusic.setOnClickListener {
            startActivity(Intent(this, MusicActivity::class.java))
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }

        btnRecord.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        btnPlay.setOnClickListener {
            playAudio()
        }

        btnSave.setOnClickListener {
            saveAudio()
        }

        switchReverse = findViewById(R.id.swReverse)
        reversedFile = File(cacheDir, "audio_reverse.wav")

        switchReverse.setOnCheckedChangeListener { _, isChecked ->
            player?.release()
            player = null
            isPlaying = false
            btnPlay.setImageResource(android.R.drawable.ic_media_play)
        }
    }

    private fun reverseAudio(): File {
        if (!wavFile.exists()) return wavFile

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

        return reversedFile
    }

    private fun clearAudio() {
        if (wavFile.exists()) wavFile.delete()
    }

    private fun startRecording() {

        clearAudio()

        // resetear reproducción previa
        player?.release()
        player = null
        isPlaying = false

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
        btnRecord.setImageResource(R.drawable.ic_stop)

        txtRecording.visibility = View.VISIBLE

        thread { writeFile(bufferSize) }
    }

    private fun stopRecording() {
        isRecordingThread = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        btnRecord.setImageResource(android.R.drawable.ic_btn_speak_now)

        isRecording = false
        txtRecording.visibility = View.GONE
    }

    private fun writeFile(bufferSize: Int) {
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

        // actualizar header
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

    private fun playAudio() {

        if (!wavFile.exists()) {
            Toast.makeText(this, "No hay audio", Toast.LENGTH_SHORT).show()
            return
        }

        val file = if (switchReverse.isChecked) {
            reverseAudio()
        } else {
            wavFile
        }

        if (player == null) {

            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
            }

            isPlaying = true
            btnPlay.setImageResource(android.R.drawable.ic_media_pause)

            // resetear cuando termina
            player?.setOnCompletionListener {
                it.release()
                player = null
                isPlaying = false
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            }

        } else {
            if (isPlaying) {
                player?.pause()
                isPlaying = false
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
            } else {
                player?.start()
                isPlaying = true
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    private fun saveAudio() {

        if (!wavFile.exists()) {
            Toast.makeText(this, "No hay audio", Toast.LENGTH_SHORT).show()
            return
        }

        // generar invertido SOLO si se necesita
        val source = if (switchReverse.isChecked) {
            reverseAudio()
            reversedFile
        } else {
            wavFile
        }

        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Guardar")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->

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
                    Toast.makeText(this, "Guardado", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.release()
        clearAudio()
    }
}
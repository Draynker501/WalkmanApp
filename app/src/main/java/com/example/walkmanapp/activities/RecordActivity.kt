package com.example.walkmanapp.activities

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.*
import android.os.*
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.walkmanapp.views.CassetteView
import com.example.walkmanapp.R
import com.example.walkmanapp.RecordSessionManager
import com.example.walkmanapp.views.WaveformView
import java.io.*
import java.lang.Short
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.min

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

    private lateinit var seekBarRecord: SeekBar
    private lateinit var txtCurrentTimeRecord: TextView
    private lateinit var txtDurationRecord: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null

    private var recordStartTime = 0L
    private var recordRunnable: Runnable? = null

    private var isUserTouching = false

    private lateinit var btnModeRecord: Button

    private lateinit var scrollView: HorizontalScrollView
    private lateinit var waveformView: WaveformView

    private lateinit var cassetteView: CassetteView

    private var lastProgress = 0

    private var pendingSeek = 0

    private var audioDuration = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record)

        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlayRecord)
        btnSave = findViewById(R.id.btnSave)
        btnModeMusic = findViewById(R.id.btnModeMusic)
        txtRecording = findViewById(R.id.txtRecording)

        seekBarRecord = findViewById(R.id.seekBarRecord)
        txtCurrentTimeRecord = findViewById(R.id.txtCurrentTimeRecord)
        txtDurationRecord = findViewById(R.id.txtDurationRecord)

        waveformView = findViewById(R.id.waveformView)
        scrollView = findViewById(R.id.waveScroll)

        btnModeRecord = findViewById(R.id.btnModeRecord)

        cassetteView = findViewById(R.id.cassetteView)

        if (RecordSessionManager.waveformAmplitudes.isNotEmpty()) {

            waveformView.setWaveform(
                RecordSessionManager.waveformAmplitudes
            )
        }

        if (RecordSessionManager.audioDuration > 0) {

            audioDuration = RecordSessionManager.audioDuration

            seekBarRecord.max = audioDuration

            txtDurationRecord.text = formatTime(audioDuration)
        }

        pendingSeek = RecordSessionManager.pendingSeek

        seekBarRecord.progress = pendingSeek

        // actualizar progress barx
        if (audioDuration > 0) {

            val progressFloat = pendingSeek.toFloat() / audioDuration

            cassetteView.setProgress(progressFloat)
        }

        txtCurrentTimeRecord.text = formatTime(pendingSeek)

        seekBarRecord.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    pendingSeek = progress
                    RecordSessionManager.pendingSeek = progress
                    player?.seekTo(progress)

                    // actualizar tiempo
                    txtCurrentTimeRecord.text = formatTime(progress)

                    // actualizar cassette
                    val duration = if (player != null) {
                        player!!.duration
                    } else {
                        audioDuration
                    }

                    if (duration > 0) {
                        val progressFloat = (progress.toFloat() / duration)
                            .coerceIn(0f, 1f)

                        cassetteView.setProgress(progressFloat)
                    }

                    // detectar dirección
                    val direction = when {
                        progress > lastProgress -> 1   // adelante
                        progress < lastProgress -> -1  // atrás (rebobinar)
                        else -> 0
                    }

                    cassetteView.updateRotation(direction)

                    lastProgress = progress
                }
            }

            override fun onStartTrackingTouch(sb: SeekBar?) {}

            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // cargar archivo en caso de que exista
        if (RecordSessionManager.audioFile != null) {

            wavFile = RecordSessionManager.audioFile!!

        } else {

            wavFile = File(cacheDir, "audio.wav")

            RecordSessionManager.audioFile = wavFile
        }

        // eliminar archivo si existe y no hay waveform
        if (
            wavFile.exists() &&
            RecordSessionManager.waveformAmplitudes.isEmpty() &&
            !RecordSessionManager.wasRecorded
        ) {

            wavFile.delete()

            File(cacheDir, "audio_reverse.wav").delete()
        }

        // cargar audio en caso de que exista
        if (!wavFile.exists()) {

            clearSession()

            waveformView.clear()

            txtCurrentTimeRecord.text = "00:00"
            txtDurationRecord.text = "00:00"

            seekBarRecord.progress = 0
            seekBarRecord.max = 0
        }

        // cargar waveform en caso de que exista, sino limpiar variables de sesión
        if (RecordSessionManager.waveformAmplitudes.isEmpty()) {
            clearSession()
        }

        btnModeMusic.setOnClickListener {
            finish()
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

        scrollView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> isUserTouching = true
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> isUserTouching = false
            }
            false
        }

        switchReverse = findViewById(R.id.swReverse)
        // cargar estado de switch en RecordSessionManager
        switchReverse.isChecked = RecordSessionManager.reverseEnabled

        // cargar archivo invertido en RecordSessionManager
        if (RecordSessionManager.reversedFile != null) {

            reversedFile = RecordSessionManager.reversedFile!!

        } else {

            reversedFile = File(cacheDir, "audio_reverse.wav")

            RecordSessionManager.reversedFile = reversedFile
        }

        switchReverse.setOnCheckedChangeListener { _, isChecked ->
            cassetteView.setReversed(isChecked)

            cassetteView.setProgress(0f)

            cassetteView.updateRotation()

            player?.let {
                RecordSessionManager.pendingSeek = it.currentPosition
            }
            player?.release()
            player = null
            isPlaying = false
            seekBarRecord.progress = 0
            txtCurrentTimeRecord.text = "00:00"
            btnPlay.setImageResource(android.R.drawable.ic_media_play)

            pendingSeek = 0
            RecordSessionManager.pendingSeek = 0

            // guardar estado de switch en RecordSessionManager
            RecordSessionManager.reverseEnabled = isChecked
        }

        // Simular que el botón está presionado por estar en RecordActivity
        btnModeRecord.post {
            btnModeRecord.animate()
                .translationX(-25f) // se mete hacia adentro
                .setDuration(120)
                .start()

            val params = btnModeRecord.layoutParams
            params.width = dpToPx(28)
            btnModeRecord.layoutParams = params
        }

        btnModeRecord.isSelected = true
    }

    private fun formatTime(ms: Int): String {
        val sec = ms / 1000
        return String.format("%02d:%02d", sec / 60, sec % 60)
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

    private fun clearReversedAudio() {
        if (reversedFile.exists()) reversedFile.delete()
    }

    private fun startRecording() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permiso de micrófono requerido", Toast.LENGTH_SHORT).show()
            return
        }

        waveformView.clear()
        //Limpiar waveform en RecordSessionManager
        RecordSessionManager.waveformAmplitudes.clear()
        clearAudio()
        clearReversedAudio()

        // reiniciar UI
        pendingSeek = 0
        RecordSessionManager.pendingSeek = 0
        seekBarRecord.progress = 0
        cassetteView.setProgress(0f)
        txtCurrentTimeRecord.text = "00:00"
        txtDurationRecord.text = "00:00"
        seekBarRecord.progress = 0
        seekBarRecord.max = 0
        lastProgress = 0

        // reiniciar reproducción previa
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

        recordStartTime = System.currentTimeMillis()
        startRecordTimer()

        thread { writeFile(bufferSize) }

        // reiniciar variables de sesión de grabación
        pendingSeek = 0
        RecordSessionManager.pendingSeek = 0

        seekBarRecord.progress = 0
        txtCurrentTimeRecord.text = "00:00"

        cassetteView.setProgress(0f)

        lastProgress = 0

        RecordSessionManager.wasRecorded = false
        RecordSessionManager.pendingSeek = 0

        // limpiar archivos en RecordSessionManager
        RecordSessionManager.reversedFile?.delete()
        RecordSessionManager.reversedFile = null
    }

    private fun stopRecording() {
        isRecordingThread = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        btnRecord.setImageResource(android.R.drawable.ic_btn_speak_now)

        isRecording = false
        txtRecording.visibility = View.GONE

        stopRecordTimer()

        val duration = (System.currentTimeMillis() - recordStartTime).toInt()
        audioDuration = duration

        //Guardar duration en RecordSessionManager
        RecordSessionManager.audioDuration = duration
        txtDurationRecord.text = formatTime(duration)
        seekBarRecord.max = duration


        RecordSessionManager.wasRecorded = true
    }

    private fun startRecordTimer() {
        recordRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - recordStartTime

                txtCurrentTimeRecord.text = formatTime(elapsed.toInt())

                /* Por si quiero implementar un fake progress bar en la animación al grabar
                val fakeProgress = (elapsed / 60000f).coerceIn(0f, 1f)
                cassetteView.setProgress(fakeProgress)
                cassetteView.updateRotation()
                */
                handler.postDelayed(this, 16)            }
        }
        handler.post(recordRunnable!!)
    }

    private fun stopRecordTimer() {
        recordRunnable?.let { handler.removeCallbacks(it) }
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
                // calcular amplitud REAL
                var maxAmp = 0
                var i = 0

                while (i < read - 1) {
                    val value = (buffer[i].toInt() or (buffer[i + 1].toInt() shl 8)).toShort()
                    val absValue = abs(value.toInt())

                    if (absValue > maxAmp) maxAmp = absValue

                    i += 2
                }

                val normalized = min(1f, maxAmp / 32767f)

                runOnUiThread {
                    waveformView.addAmplitude(normalized)
                    //Guardar waveform en RecordSessionManager
                    RecordSessionManager.waveformAmplitudes.add(normalized)

                    if (!isUserTouching) {
                        scrollView.post {
                            scrollView.scrollTo(waveformView.width, 0)
                        }
                    }
                }
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
        file.writeShort(Short.reverseBytes(1).toInt())
        file.writeShort(Short.reverseBytes(1).toInt())
        file.writeInt(Integer.reverseBytes(44100))
        file.writeInt(Integer.reverseBytes(44100 * 2))
        file.writeShort(Short.reverseBytes(2).toInt())
        file.writeShort(Short.reverseBytes(16).toInt())
        file.writeBytes("data")
        file.writeInt(0)
    }

    private fun playAudio() {

        if (!wavFile.exists()) {
            Toast.makeText(this, "No hay audio", Toast.LENGTH_SHORT).show()
            return
        }

        val file = if (switchReverse.isChecked) {
            if (!reversedFile.exists()) {
                reverseAudio()
            }
            reversedFile
        } else {
            wavFile
        }

        if (player == null) {

            player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                seekTo(pendingSeek)
            }

            seekBarRecord.max = player!!.duration
            txtDurationRecord.text = formatTime(player!!.duration)

            cassetteView.setProgress(0f)
            cassetteView.invalidate()

            handler.postDelayed({
                seekBarRecord.max = player!!.duration
            }, 200)

            player?.start()
            isPlaying = true
            btnPlay.setImageResource(android.R.drawable.ic_media_pause)

            runnable = object : Runnable {
                override fun run() {

                    player?.let { mp ->

                        if (mp.isPlaying) {

                            val pos = mp.currentPosition.coerceAtMost(mp.duration)

                            seekBarRecord.progress = pos
                            txtCurrentTimeRecord.text = formatTime(pos)

                            // actualizar progress bar en RecordSessionManager
                            pendingSeek = pos
                            RecordSessionManager.pendingSeek = pos

                            val progress = pos.toFloat() / mp.duration

                            cassetteView.setProgress(progress)
                            cassetteView.updateRotation()
                        }

                        handler.postDelayed(this, 16)
                    }
                }
            }

            handler.post(runnable!!)

            player?.setOnCompletionListener {
                it.release()
                player = null
                isPlaying = false
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
                seekBarRecord.progress = 0
                txtCurrentTimeRecord.text = "00:00"
                cassetteView.setProgress(0f)
                runnable?.let { r -> handler.removeCallbacks(r) }
                pendingSeek = 0
                RecordSessionManager.pendingSeek = 0
            }

        } else {
            if (isPlaying) {
                player?.pause()
                isPlaying = false
                btnPlay.setImageResource(android.R.drawable.ic_media_play)
                runnable?.let {
                    handler.removeCallbacks(it)
                }            } else {
                player?.start()
                isPlaying = true
                btnPlay.setImageResource(android.R.drawable.ic_media_pause)
                runnable?.let {
                    handler.post(it)
                }            }
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
        runnable?.let { handler.removeCallbacks(it) }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    // limpiar variables de sesión
    private fun clearSession() {

        RecordSessionManager.audioFile = null
        RecordSessionManager.reversedFile = null

        RecordSessionManager.pendingSeek = 0
        RecordSessionManager.audioDuration = 0

        RecordSessionManager.wasRecorded = false
        RecordSessionManager.reverseEnabled = false

        RecordSessionManager.waveformAmplitudes.clear()
    }
}
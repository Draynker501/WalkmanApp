package com.example.walkmanapp

import java.io.File

object RecordSessionManager {

    var audioFile: File? = null

    var reversedFile: File? = null

    var pendingSeek = 0

    var audioDuration = 0

    var wasRecorded = false

    var reverseEnabled = false

    var waveformAmplitudes = mutableListOf<Float>()
}
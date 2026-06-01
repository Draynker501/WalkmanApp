package com.example.walkmanapp.models

import android.graphics.Bitmap

data class Song(
    val title: String,
    val artist: String,
    val album: String,
    val duration: String,
    val path: String,
    val art: Bitmap? = null,
    var isFavorite: Boolean = false,
    var lyrics: String? = null,
    var lyricsFile: String? = null,
    val genre: String = "Desconocido",
    var playCount: Int = 0,
    var lastPlayed: Long = 0L,
    val dateAdded: Long
)
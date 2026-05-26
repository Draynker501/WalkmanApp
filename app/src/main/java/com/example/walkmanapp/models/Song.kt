package com.example.walkmanapp.models

import android.graphics.Bitmap

data class Song(
    val title: String,
    val artist: String,
    val path: String,
    val art: Bitmap? = null
)
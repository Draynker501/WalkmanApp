package com.example.walkmanapp.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.walkmanapp.models.Song

class MusicViewModel : ViewModel() {

    val selectedSong = MutableLiveData<Song>()
}
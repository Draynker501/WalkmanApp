package com.example.walkmanapp.fragments

import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walkmanapp.R
import com.example.walkmanapp.adapters.SongsAdapter
import com.example.walkmanapp.models.Song
import androidx.lifecycle.ViewModelProvider
import com.example.walkmanapp.viewmodels.MusicViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class SongsFragment : Fragment() {

    private lateinit var recyclerSongs: RecyclerView
    private lateinit var searchSongs: EditText

    private lateinit var adapter: SongsAdapter

    private val songsList = mutableListOf<Song>()
    private val filteredList = mutableListOf<Song>()

    private lateinit var musicViewModel: MusicViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_songs,
            container,
            false
        )

        recyclerSongs = view.findViewById(R.id.recyclerSongs)
        searchSongs = view.findViewById(R.id.searchSongs)

        recyclerSongs.layoutManager = LinearLayoutManager(requireContext())

        musicViewModel = ViewModelProvider(requireActivity())[MusicViewModel::class.java]

        loadSongs()

        musicViewModel.songsList.value = songsList

        adapter = SongsAdapter(filteredList) { song ->

            val index = songsList.indexOf(song)

            musicViewModel.currentIndex = index

            musicViewModel.selectedSong.value = song

            requireActivity()
                .findViewById<BottomNavigationView>(R.id.bottomNavigation)
                .selectedItemId = R.id.nav_player
        }

        recyclerSongs.adapter = adapter

        setupSearch()

        return view
    }

    private fun loadSongs() {

        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor: Cursor? = requireContext().contentResolver.query(
            collection,
            projection,
            selection,
            null,
            MediaStore.Audio.Media.TITLE + " ASC"
        )

        cursor?.use {

            val titleColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

            val artistColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            val pathColumn =
                it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {

                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val path = it.getString(pathColumn)

                songsList.add(
                    Song(
                        title,
                        artist,
                        path
                    )
                )
            }
        }

        filteredList.addAll(songsList)
    }

    private fun setupSearch() {

        searchSongs.addTextChangedListener(object : TextWatcher {

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {

                filterSongs(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterSongs(query: String) {

        filteredList.clear()

        if (query.isEmpty()) {

            filteredList.addAll(songsList)

        } else {

            val result = songsList.filter {

                it.title.contains(query, true) ||
                        it.artist.contains(query, true)
            }

            filteredList.addAll(result)
        }

        adapter.updateList(filteredList)
    }
}
package com.example.walkmanapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walkmanapp.R
import com.example.walkmanapp.adapters.SongsAdapter
import com.example.walkmanapp.models.Song
import android.database.Cursor
import android.provider.MediaStore
import androidx.lifecycle.lifecycleScope
import com.example.walkmanapp.DatabaseProvider
import kotlinx.coroutines.launch

private lateinit var recycler: RecyclerView

private val playlistSongs = mutableListOf<Song>()
private var playlistId = 0L

private var playlistName = ""
class PlaylistDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_playlist_detail,
            container,
            false
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        playlistId =
            arguments?.getLong("playlistId")
                ?: 0L

        playlistName =
            arguments?.getString("playlistName")
                ?: ""
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        view.findViewById<TextView>(
            R.id.playlistTitle
        ).text = playlistName

        recycler =
            view.findViewById(
                R.id.songsRecycler
            )

        recycler.layoutManager =
            LinearLayoutManager(requireContext())

        recycler.adapter =
            SongsAdapter(

                playlistSongs,

                onSongClick = {},

                onMenuClick = { _, _ -> }
            )

        loadPlaylistSongs()
    }

    private fun loadPlaylistSongs() {

        lifecycleScope.launch {

            val db =
                DatabaseProvider.getDatabase(
                    requireContext()
                )

            val playlistSongEntities =
                db.playlistSongDao()
                    .getSongs(playlistId)

            playlistSongs.clear()

            for (playlistSong in playlistSongEntities) {

                val song =
                    getSongFromPath(
                        playlistSong.songPath
                    )

                if (song != null) {

                    playlistSongs.add(song)
                }
            }

            recycler.adapter?.notifyDataSetChanged()
        }
    }

    private fun getSongFromPath(
        songPath: String
    ): Song? {

        val collection =
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(

            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION
        )

        val selection =
            "${MediaStore.Audio.Media.DATA} = ?"

        val selectionArgs =
            arrayOf(songPath)

        val cursor: Cursor? =
            requireContext()
                .contentResolver
                .query(

                    collection,
                    projection,
                    selection,
                    selectionArgs,
                    null
                )

        cursor?.use {

            if (it.moveToFirst()) {

                val title =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.TITLE
                        )
                    )

                val artist =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.ARTIST
                        )
                    )

                val path =
                    it.getString(
                        it.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DATA
                        )
                    )

                val durationMs =
                    it.getLong(
                        it.getColumnIndexOrThrow(
                            MediaStore.Audio.Media.DURATION
                        )
                    )

                return Song(

                    title = title,

                    artist = artist,

                    duration = formatDuration(
                        durationMs
                    ),

                    path = path
                )
            }
        }

        return null
    }

    private fun formatDuration(durationMs: Long): String {

        val totalSeconds =
            durationMs / 1000

        val minutes =
            totalSeconds / 60

        val seconds =
            totalSeconds % 60

        return String.format(
            "%d:%02d",
            minutes,
            seconds
        )
    }
}
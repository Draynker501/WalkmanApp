package com.example.walkmanapp.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.walkmanapp.DatabaseProvider
import com.example.walkmanapp.R
import com.example.walkmanapp.adapters.PlaylistsAdapter
import com.example.walkmanapp.models.PlaylistEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
class PlaylistsFragment : Fragment() {

    private lateinit var adapter: PlaylistsAdapter

    private val playlists =
        mutableListOf<PlaylistEntity>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        return inflater.inflate(
            R.layout.fragment_playlists,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val recycler =
            view.findViewById<RecyclerView>(
                R.id.playlistsRecycler
            )

        recycler.layoutManager =
            LinearLayoutManager(requireContext())

        adapter =
            PlaylistsAdapter(playlists) {

                openPlaylist(it)
            }

        recycler.adapter = adapter

        lifecycleScope.launch {

            ensureFavoritesPlaylist()

            loadPlaylists()
        }

        val addButton =
            view.findViewById<FloatingActionButton>(
                R.id.addPlaylistButton
            )

        addButton.setOnClickListener {

            CreatePlaylistDialogFragment {

                    playlistName ->

                lifecycleScope.launch {

                    val db =
                        DatabaseProvider.getDatabase(
                            requireContext()
                        )

                    db.playlistDao()
                        .insertPlaylist(

                            PlaylistEntity(
                                name = playlistName
                            )
                        )

                    loadPlaylists()
                }

            }.show(
                parentFragmentManager,
                "create_playlist"
            )
        }
    }

    private suspend fun ensureFavoritesPlaylist() {

        val db =
            DatabaseProvider.getDatabase(
                requireContext()
            )

        val playlistDao =
            db.playlistDao()

        val favorites =
            playlistDao.getFavoritesPlaylist()

        if (favorites == null) {

            playlistDao.insertPlaylist(

                PlaylistEntity(
                    name = "Favoritos",
                    isSystem = true
                )
            )
        }
    }

    private fun loadPlaylists() {

        lifecycleScope.launch {

            val db =
                DatabaseProvider.getDatabase(
                    requireContext()
                )

            playlists.clear()

            playlists.addAll(

                db.playlistDao()
                    .getAllPlaylists()
            )

            adapter.notifyDataSetChanged()
        }
    }

    class CreatePlaylistDialogFragment(
        private val onPlaylistCreated: (String) -> Unit
    ) : DialogFragment() {

        override fun onCreateDialog(
            savedInstanceState: Bundle?
        ): Dialog {

            val view =
                requireActivity()
                    .layoutInflater
                    .inflate(
                        R.layout.dialog_create_playlist,
                        null
                    )

            val input =
                view.findViewById<EditText>(
                    R.id.playlistNameInput
                )

            return AlertDialog.Builder(requireContext())
                .setTitle("Nueva playlist")
                .setView(view)
                .setPositiveButton("Crear") { _, _ ->

                    val name =
                        input.text.toString()

                    if (name.isNotBlank()) {
                        onPlaylistCreated(name)
                    }
                }
                .setNegativeButton("Cancelar", null)
                .create()
        }
    }

    private fun openPlaylist(
        playlist: PlaylistEntity
    ) {

        val fragment =
            PlaylistDetailFragment()

        fragment.arguments =
            Bundle().apply {

                putLong(
                    "playlistId",
                    playlist.id
                )

                putString(
                    "playlistName",
                    playlist.name
                )
            }

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .addToBackStack(null)
            .commit()
    }
}
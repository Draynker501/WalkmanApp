package com.example.walkmanapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.walkmanapp.R
import com.example.walkmanapp.models.PlaylistEntity

class PlaylistsAdapter(
    private val playlists: List<PlaylistEntity>,
    private val onClick: (PlaylistEntity) -> Unit
) : RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val playlistName: TextView =
            view.findViewById(R.id.playlistName)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_playlist,
                    parent,
                    false
                )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val playlist = playlists[position]

        holder.playlistName.text =
            playlist.name

        holder.itemView.setOnClickListener {
            onClick(playlist)
        }
    }

    override fun getItemCount(): Int {
        return playlists.size
    }
}
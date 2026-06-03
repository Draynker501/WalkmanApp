package com.example.walkmanapp

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.walkmanapp.models.PlaylistDao
import com.example.walkmanapp.models.PlaylistEntity
import com.example.walkmanapp.models.PlaylistSongDao
import com.example.walkmanapp.models.PlaylistSongEntity

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistSongEntity::class
    ],
    version = 1
)
abstract class WalkmanDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    abstract fun playlistSongDao(): PlaylistSongDao
}
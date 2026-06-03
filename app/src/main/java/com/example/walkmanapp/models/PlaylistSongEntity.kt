package com.example.walkmanapp.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songPath"]
)
data class PlaylistSongEntity(

    val playlistId: Long,

    val songPath: String,

    val addedAt: Long = System.currentTimeMillis()
)

@Dao
interface PlaylistSongDao {

    @Query(
        """
        SELECT * FROM playlist_songs
        WHERE playlistId = :playlistId
        """
    )
    suspend fun getSongs(
        playlistId: Long
    ): List<PlaylistSongEntity>

    @Query("""
SELECT COUNT(*) > 0
FROM playlist_songs
WHERE playlistId = :playlistId
AND songPath = :songPath
""")
    suspend fun exists(
        playlistId: Long,
        songPath: String
    ): Boolean

    @Insert
    suspend fun addSong(
        song: PlaylistSongEntity
    )

    @Delete
    suspend fun removeSong(
        song: PlaylistSongEntity
    )
}
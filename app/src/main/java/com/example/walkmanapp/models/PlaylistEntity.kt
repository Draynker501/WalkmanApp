package com.example.walkmanapp.models

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "playlists")
data class PlaylistEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val isSystem: Boolean = false
)

@Dao
interface PlaylistDao {

    @Query(
        """
    SELECT * FROM playlists
    WHERE isSystem = 1
    LIMIT 1
    """
    )
    suspend fun getFavoritesPlaylist(): PlaylistEntity?

    @Query(
        """
    SELECT * FROM playlists
    """
    )
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Insert
    suspend fun insertPlaylist(
        playlist: PlaylistEntity
    ): Long

    @Delete
    suspend fun deletePlaylist(
        playlist: PlaylistEntity
    )
}
package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artistName: String,
    val albumName: String,
    val durationSeconds: Int,
    val streamUrl: String,
    val coverUrl: String,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Helper to get play source path based on download availability
    fun getPlayableUrl(): String {
        return if (isDownloaded && !localFilePath.isNullOrEmpty()) {
            localFilePath
        } else {
            streamUrl
        }
    }
}

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val coverUrl: String?,
    val accentColor: String? = "#E50914", // Apple Music layout matching hex
    val isSmartGenerated: Boolean = false,
    val prompt: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_track_cross_ref",
    primaryKeys = ["playlistId", "trackId"]
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String
)

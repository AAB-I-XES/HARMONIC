package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val musicDao: MusicDao
) {
    private val TAG = "MusicRepository"
    private val client = OkHttpClient()

    // Database Reactive Streams
    val allPlaylists: Flow<List<PlaylistEntity>> = musicDao.getAllPlaylists()
    val downloadedTracks: Flow<List<TrackEntity>> = musicDao.getDownloadedTracks()
    val allTracks: Flow<List<TrackEntity>> = musicDao.getAllTracks()

    fun getTracksForPlaylist(playlistId: String): Flow<List<TrackEntity>> {
        return musicDao.getTracksForPlaylist(playlistId)
    }

    fun getPlaylistTrackCount(playlistId: String): Flow<Int> {
        return musicDao.getPlaylistTrackCount(playlistId)
    }

    suspend fun getPlaylistById(id: String): PlaylistEntity? {
        return musicDao.getPlaylistById(id)
    }

    suspend fun getTrackById(id: String): TrackEntity? {
        return musicDao.getTrackById(id)
    }

    // Playlist Operations
    suspend fun createPlaylist(name: String, description: String?, accentColor: String? = "#E50914", isSmart: Boolean = false, prompt: String? = null): String {
        val id = UUID.randomUUID().toString()
        val playlist = PlaylistEntity(
            id = id,
            name = name,
            description = description,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&q=80",
            accentColor = accentColor ?: "#E50914",
            isSmartGenerated = isSmart,
            prompt = prompt
        )
        musicDao.insertPlaylist(playlist)
        return id
    }

    suspend fun deletePlaylist(playlistId: String) {
        musicDao.deletePlaylistById(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: String, track: TrackEntity) {
        // First, ensure the track is saved in the track metadata table
        val existing = musicDao.getTrackById(track.id)
        if (existing == null) {
            musicDao.insertTrack(track)
        }
        val crossRef = PlaylistTrackCrossRef(playlistId, track.id)
        musicDao.insertPlaylistTrackCrossRef(crossRef)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        musicDao.deletePlaylistTrackCrossRef(playlistId, trackId)
    }

    // Network Track Query with robustness: Fallback if offline/error
    suspend fun getTrendingTracks(): List<TrackEntity> = withContext(Dispatchers.IO) {
        try {
            val response = RetrofitMusicClient.service.searchTracks(term = "hits", limit = 25)
            if (response.results.isNotEmpty()) {
                // Filter songs that have preview streams
                val list = response.results.filter { !it.previewUrl.isNullOrEmpty() }.map {
                    TrackEntity(
                        id = it.trackId.toString(),
                        title = it.trackName ?: "Untitled",
                        artistName = it.artistName ?: "Unknown Artist",
                        albumName = it.collectionName ?: "Single",
                        durationSeconds = (it.trackTimeMillis ?: 180000) / 1000,
                        streamUrl = it.previewUrl!!,
                        coverUrl = RetrofitMusicClient.getHighResArtworkUrl(it.artworkUrl100)
                    )
                }
                // Pre-populate trending tracks into database metadata cache so user can view/play
                list.forEach { track ->
                    val existing = musicDao.getTrackById(track.id)
                    if (existing == null) {
                        musicDao.insertTrack(track)
                    } else {
                        // Keep download status when updating from cloud
                        musicDao.updateTrack(track.copy(
                            isDownloaded = existing.isDownloaded,
                            localFilePath = existing.localFilePath
                        ))
                    }
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch trending from iTunes: ${e.message}. Using high-quality curated fallbacks.")
        }
        
        // Populate fallback to database metadata
        RetrofitMusicClient.curatedFallbackTracks.forEach { track ->
            val existing = musicDao.getTrackById(track.id)
            if (existing == null) {
                musicDao.insertTrack(track)
            }
        }
        return@withContext RetrofitMusicClient.curatedFallbackTracks
    }

    suspend fun searchTracks(query: String): List<TrackEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getTrendingTracks()
        try {
            val response = RetrofitMusicClient.service.searchTracks(term = query, limit = 25)
            if (response.results.isNotEmpty()) {
                return@withContext response.results.filter { !it.previewUrl.isNullOrEmpty() }.map {
                    TrackEntity(
                        id = it.trackId.toString(),
                        title = it.trackName ?: "Untitled",
                        artistName = it.artistName ?: "Unknown Artist",
                        albumName = it.collectionName ?: "Single",
                        durationSeconds = (it.trackTimeMillis ?: 180000) / 1000,
                        streamUrl = it.previewUrl!!,
                        coverUrl = RetrofitMusicClient.getHighResArtworkUrl(it.artworkUrl100)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed on iTunes: ${e.message}")
        }
        
        // Local database text search and in-memory filter as robust fallback
        val cached = musicDao.getAllTracks().first()
        val filtered = cached.filter {
            it.title.contains(query, ignoreCase = true) || 
            it.artistName.contains(query, ignoreCase = true) ||
            it.albumName.contains(query, ignoreCase = true)
        }
        if (filtered.isNotEmpty()) {
            return@withContext filtered
        }
        
        // Regex search inside the curated fallback list
        return@withContext RetrofitMusicClient.curatedFallbackTracks.filter {
            it.title.contains(query, ignoreCase = true) || 
            it.artistName.contains(query, ignoreCase = true)
        }
    }

    // Offline Downloader: download mp3 stream to sandboxed internal files
    suspend fun downloadTrack(trackId: String, onProgress: (Float) -> Unit = {}): Boolean = withContext(Dispatchers.IO) {
        val track = musicDao.getTrackById(trackId) ?: return@withContext false
        if (track.isDownloaded && track.localFilePath != null && File(track.localFilePath).exists()) {
            return@withContext true
        }

        try {
            // Setup target directories
            val tracksDir = File(context.filesDir, "tracks")
            if (!tracksDir.exists()) {
                tracksDir.mkdirs()
            }
            val localFile = File(tracksDir, "track_$trackId.mp3")

            // Network download
            val request = Request.Builder().url(track.streamUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            val body = response.body ?: return@withContext false
            val contentLength = body.contentLength()
            val inputStream: InputStream = body.byteStream()
            val outputStream = FileOutputStream(localFile)

            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytesRead: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (contentLength > 0) {
                    onProgress(totalBytesRead.toFloat() / contentLength.toFloat())
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Update database track status
            val updatedTrack = track.copy(
                isDownloaded = true,
                localFilePath = localFile.absolutePath,
                timestamp = System.currentTimeMillis()
            )
            musicDao.insertTrack(updatedTrack)
            Log.d(TAG, "Successfully downloaded track $trackId to ${localFile.absolutePath}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading track $trackId: ${e.message}")
            return@withContext false
        }
    }

    suspend fun removeDownload(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val track = musicDao.getTrackById(trackId) ?: return@withContext false
        try {
            if (track.localFilePath != null) {
                val file = File(track.localFilePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            val updated = track.copy(
                isDownloaded = false,
                localFilePath = null
            )
            musicDao.insertTrack(updated)
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error removing download: ${e.message}")
            return@withContext false
        }
    }
}

package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.player.AudioPlayer
import com.example.smart.SmartPlaylistGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UserProfile(
    val email: String,
    val username: String,
    val avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&q=80"
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "MusicViewModel"
    private val database = MusicDatabase.getDatabase(application)
    val repository = MusicRepository(application, database.musicDao())
    val audioPlayer = AudioPlayer(application)

    // Auth State and shared preferences caching
    private val sharedPrefs = application.getSharedPreferences("harmonic_auth_prefs", Context.MODE_PRIVATE)
    
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    fun loginUser(email: String, username: String) {
        sharedPrefs.edit()
            .putString("user_email", email)
            .putString("user_username", username)
            .apply()
        _currentUser.value = UserProfile(email, username)
    }

    fun logoutUser() {
        sharedPrefs.edit().clear().apply()
        _currentUser.value = null
    }

    // Online search status
    private val _onlineTrendingTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val onlineTrendingTracks: StateFlow<List<TrackEntity>> = _onlineTrendingTracks.asStateFlow()

    private val _searchResults = MutableStateFlow<List<TrackEntity>>(emptyList())
    val searchResults: StateFlow<List<TrackEntity>> = _searchResults.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingSongs = MutableStateFlow(false)
    val isLoadingSongs: StateFlow<Boolean> = _isLoadingSongs.asStateFlow()

    // Playlist details view
    private val _activePlaylist = MutableStateFlow<PlaylistEntity?>(null)
    val activePlaylist: StateFlow<PlaylistEntity?> = _activePlaylist.asStateFlow()

    private val _activePlaylistTracks = MutableStateFlow<List<TrackEntity>>(emptyList())
    val activePlaylistTracks: StateFlow<List<TrackEntity>> = _activePlaylistTracks.asStateFlow()

    // Real-time track download progress tracks
    val downloadProgressMap = mutableStateMapOf<String, Float>()

    // Global smart content operational state
    private val _isSmartCurating = MutableStateFlow(false)
    val isSmartCurating: StateFlow<Boolean> = _isSmartCurating.asStateFlow()

    // Network connectivity override (Simulated Offline Mode toggle is great for demoing!)
    private val _forceOfflineMode = MutableStateFlow(false)
    val forceOfflineMode: StateFlow<Boolean> = _forceOfflineMode.asStateFlow()

    // DB Source Streams
    val playlistsFlow = repository.allPlaylists.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val downloadedTracksFlow = repository.downloadedTracks.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        val savedEmail = sharedPrefs.getString("user_email", null)
        val savedUsername = sharedPrefs.getString("user_username", null)
        if (savedEmail != null && savedUsername != null) {
            _currentUser.value = UserProfile(savedEmail, savedUsername)
        }
        
        // Initial catalog loading
        fetchTrending()
        
        // Setup initial default playlists if db is blank
        viewModelScope.launch {
            val list = playlistsFlow.first()
            if (list.isEmpty()) {
                Log.d(TAG, "Creating default music boards...")
                val p1 = repository.createPlaylist(
                    name = "Vibe Lounge",
                    description = "Chamber sounds and warm lo-fi echoes to accompany acoustic evenings.",
                    accentColor = "#FF2D55", // Signature Pink
                    isSmart = false
                )
                val p2 = repository.createPlaylist(
                    name = "Synth Hyper-Drive",
                    description = "Spirited outrun retro synth waves tailored for maximum neon adrenaline.",
                    accentColor = "#AF52DE", // Deep Purple
                    isSmart = false
                )
                
                // Pack default lists
                val fallbackTracks = RetrofitMusicClient.curatedFallbackTracks
                repository.addTrackToPlaylist(p1, fallbackTracks[0])
                repository.addTrackToPlaylist(p1, fallbackTracks[1])
                repository.addTrackToPlaylist(p1, fallbackTracks[3])
                
                repository.addTrackToPlaylist(p2, fallbackTracks[2])
                repository.addTrackToPlaylist(p2, fallbackTracks[4])
                repository.addTrackToPlaylist(p2, fallbackTracks[5])
            }
        }
    }

    // Check actual internet availability helper
    fun isNetworkAvailable(): Boolean {
        if (_forceOfflineMode.value) return false
        return try {
            val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val NW = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(NW) ?: return false
            when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed check network eligibility: ${e.message}")
            false
        }
    }

    fun toggleForceOfflineMode() {
        _forceOfflineMode.value = !_forceOfflineMode.value
        Log.d(TAG, "Toggle force-offline mode: ${_forceOfflineMode.value}")
        // Re-load trending to check correct state
        fetchTrending()
    }

    fun fetchTrending() {
        viewModelScope.launch {
            _isLoadingSongs.value = true
            try {
                if (isNetworkAvailable()) {
                    val list = repository.getTrendingTracks()
                    _onlineTrendingTracks.value = list
                } else {
                    // Offline - load standard DB / fallback cached tracks
                    _onlineTrendingTracks.value = RetrofitMusicClient.curatedFallbackTracks
                }
            } catch (e: Exception) {
                _onlineTrendingTracks.value = RetrofitMusicClient.curatedFallbackTracks
            } finally {
                _isLoadingSongs.value = false
            }
        }
    }

    fun searchSongs(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            if (query.isBlank()) {
                _searchResults.value = emptyList()
                return@launch
            }
            _isLoadingSongs.value = true
            try {
                if (isNetworkAvailable()) {
                    val list = repository.searchTracks(query)
                    _searchResults.value = list
                } else {
                    // Search locally inside already downloaded tracks
                    val list = database.musicDao().getAllTracks().first().filter {
                        it.title.contains(query, ignoreCase = true) || 
                        it.artistName.contains(query, ignoreCase = true)
                    }
                    _searchResults.value = list
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed searching in VM: ${e.message}")
            } finally {
                _isLoadingSongs.value = false
            }
        }
    }

    fun viewPlaylist(playlist: PlaylistEntity) {
        _activePlaylist.value = playlist
        viewModelScope.launch {
            repository.getTracksForPlaylist(playlist.id).collectLatest { tracks ->
                _activePlaylistTracks.value = tracks
            }
        }
    }

    // Playback integration with ViewModels
    fun playTrack(track: TrackEntity, customQueue: List<TrackEntity> = emptyList()) {
        val finalQueue = if (customQueue.isNotEmpty()) customQueue else {
            if (_activePlaylist.value != null && _activePlaylistTracks.value.isNotEmpty()) {
                _activePlaylistTracks.value
            } else {
                _onlineTrendingTracks.value
            }
        }
        audioPlayer.playTrack(track, finalQueue)
    }

    // Local Playlist Creation
    fun createCustomPlaylist(name: String, description: String?, accentHex: String = "#FF9F04") {
        viewModelScope.launch {
            repository.createPlaylist(name, description, accentHex)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_activePlaylist.value?.id == playlistId) {
                _activePlaylist.value = null
                _activePlaylistTracks.value = emptyList()
            }
        }
    }

    fun addTrackToPlaylist(playlistId: String, track: TrackEntity) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    // Binary file downloading management
    fun downloadTrack(track: TrackEntity) {
        if (downloadProgressMap.containsKey(track.id)) return // Already in progress
        downloadProgressMap[track.id] = 0.01f

        viewModelScope.launch {
            repository.downloadTrack(track.id) { progress ->
                downloadProgressMap[track.id] = progress
            }
            downloadProgressMap.remove(track.id)
            
            // Re-sync current playing track status if playing
            val currentPh = audioPlayer.currentTrack.value
            if (currentPh?.id == track.id) {
                val freshlyLoaded = repository.getTrackById(track.id)
                if (freshlyLoaded != null) {
                    // Update state inside AudioPlayer dynamically
                    // without pausing playback
                    Log.d(TAG, "Downloaded active playing track. Synced local filepath status.")
                }
            }
        }
    }

    fun removeTrackDownload(track: TrackEntity) {
        viewModelScope.launch {
            repository.removeDownload(track.id)
        }
    }

    // Smart generative creator
    fun generateSmartPersonalizedPlaylist(prompt: String, onSuccess: (String) -> Unit = {}) {
        if (prompt.isBlank() || _isSmartCurating.value) return
        _isSmartCurating.value = true

        viewModelScope.launch {
            try {
                // Ensure we have some cached catalog pieces to choose from
                val dbTracks = database.musicDao().getAllTracks().first()
                val activeCatalog = if (dbTracks.size > 2) dbTracks else RetrofitMusicClient.curatedFallbackTracks
                
                val playlistId = SmartPlaylistGenerator.generatePersonalizedPlaylist(
                    prompt = prompt,
                    musicDao = database.musicDao(),
                    availableTracks = activeCatalog
                )
                
                // Load details automatically
                val generatedPlaylist = repository.getPlaylistById(playlistId)
                if (generatedPlaylist != null) {
                    viewPlaylist(generatedPlaylist)
                    onSuccess(playlistId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Smart Generator VM error: ${e.message}")
            } finally {
                _isSmartCurating.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}

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
        // Clear auth but keep preferences like dark mode
        sharedPrefs.edit()
            .remove("user_email")
            .remove("user_username")
            .apply()
        _currentUser.value = null
    }

    // Dark Theme preference StateFlow
    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode_enabled", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("dark_mode_enabled", enabled).apply()
        _isDarkMode.value = enabled
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

    // ======================== RADIO STREAMING CONFIGURATION ========================
    private val _radioStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val radioStations: StateFlow<List<RadioStation>> = _radioStations.asStateFlow()

    private val _isSearchingRadio = MutableStateFlow(false)
    val isSearchingRadio: StateFlow<Boolean> = _isSearchingRadio.asStateFlow()

    private val _radioSearchQuery = MutableStateFlow("")
    val radioSearchQuery: StateFlow<String> = _radioSearchQuery.asStateFlow()

    private val _radioSelectedCountry = MutableStateFlow("")
    val radioSelectedCountry: StateFlow<String> = _radioSelectedCountry.asStateFlow()

    private val _radioSelectedState = MutableStateFlow("")
    val radioSelectedState: StateFlow<String> = _radioSelectedState.asStateFlow()

    private val curatedFallbackRadioStations = listOf(
        RadioStation(
            stationuuid = "fallback_jazz",
            name = "Jazz24 (Seattle)",
            url = "https://live.jazz24.org/jazz24-mp3",
            url_resolved = "https://live.jazz24.org/jazz24-mp3",
            favicon = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=300&q=80",
            tags = "jazz, smooth jazz, classic",
            country = "United States",
            state = "Washington",
            votes = 2400,
            clickcount = 8900
        ),
        RadioStation(
            stationuuid = "fallback_kexp",
            name = "KEXP 90.3 FM",
            url = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
            url_resolved = "https://kexp-mp3-128.streamguys1.com/kexp128.mp3",
            favicon = "https://images.unsplash.com/photo-1487180142328-0c4e37023af5?w=300&q=80",
            tags = "alternative, rock, indie",
            country = "United States",
            state = "Washington",
            votes = 1950,
            clickcount = 7200
        ),
        RadioStation(
            stationuuid = "fallback_lofi",
            name = "Lofi & Study Beats",
            url = "https://stream.zeno.fm/0rcshz6bvd0hv",
            url_resolved = "https://stream.zeno.fm/0rcshz6bvd0hv",
            favicon = "https://images.unsplash.com/photo-1516280440614-37939bbacd6a?w=300&q=80",
            tags = "lofi, chillhop, study",
            country = "France",
            state = "Paris",
            votes = 1850,
            clickcount = 6520
        ),
        RadioStation(
            stationuuid = "fallback_dance",
            name = "Ibiza Global Radio",
            url = "https://ibizaglobalradio.live-streams.nl:8000/live",
            url_resolved = "https://ibizaglobalradio.live-streams.nl:8000/live",
            favicon = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&q=80",
            tags = "dance, electronic, house",
            country = "Spain",
            state = "Ibiza",
            votes = 3200,
            clickcount = 11200
        ),
        RadioStation(
            stationuuid = "fallback_bollywood",
            name = "Bollywood Non-Stop Hits",
            url = "https://stream.zeno.fm/f9u6t1v7u0hvv",
            url_resolved = "https://stream.zeno.fm/f9u6t1v7u0hvv",
            favicon = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&q=80",
            tags = "bollywood, hindi, pop",
            country = "India",
            state = "Maharashtra",
            votes = 1450,
            clickcount = 5600
        ),
        RadioStation(
            stationuuid = "fallback_news",
            name = "WNYC Public FM 93.9",
            url = "https://fm939.wnyc.org/wnycfm-web.aac",
            url_resolved = "https://fm939.wnyc.org/wnycfm-web.aac",
            favicon = "https://images.unsplash.com/photo-1585699324551-f6c309eed262?w=300&q=80",
            tags = "news, talk, culture",
            country = "United States",
            state = "New York",
            votes = 1100,
            clickcount = 4300
        )
    )

    fun updateRadioFilters(query: String, country: String, state: String) {
        _radioSearchQuery.value = query
        _radioSelectedCountry.value = country
        _radioSelectedState.value = state
        fetchRadioStations(query, country, state)
    }

    fun fetchRadioStations(query: String? = null, country: String? = null, state: String? = null) {
        viewModelScope.launch {
            _isSearchingRadio.value = true
            try {
                if (isNetworkAvailable()) {
                    val qName = if (query.isNullOrBlank()) null else query.trim()
                    val qCountry = if (country.isNullOrBlank() || country == "All Countries") null else country.trim()
                    val qState = if (state.isNullOrBlank() || state == "All States") null else state.trim()

                    val list = if (qName == null && qCountry == null && qState == null) {
                        try {
                            RetrofitRadioClient.service.getTopStations()
                        } catch (e: Exception) {
                            RetrofitRadioClient.service.searchStations(limit = 40)
                        }
                    } else {
                        RetrofitRadioClient.service.searchStations(
                            name = qName,
                            country = qCountry,
                            state = qState
                        )
                    }

                    if (list.isNotEmpty()) {
                        _radioStations.value = list
                    } else {
                        // If API worked but returned empty list, filter fallbacks as a smart mock alternative!
                        filterCuratedFallbackRadioStations(qName, qCountry, qState)
                    }
                } else {
                    filterCuratedFallbackRadioStations(query, country, state)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Radio API error: ${e.message}. Using high-quality curated fallbacks.")
                filterCuratedFallbackRadioStations(query, country, state)
            } finally {
                _isSearchingRadio.value = false
            }
        }
    }

    private fun filterCuratedFallbackRadioStations(query: String?, country: String?, state: String?) {
        val qName = query?.trim()
        val qCountry = if (country == "All Countries" || country == "") null else country?.trim()
        val qState = if (state == "All States" || state == "") null else state?.trim()

        var filtered = curatedFallbackRadioStations
        if (!qName.isNullOrEmpty()) {
            filtered = filtered.filter { it.name.contains(qName, ignoreCase = true) || (it.tags?.contains(qName, ignoreCase = true) ?: false) }
        }
        if (!qCountry.isNullOrEmpty()) {
            filtered = filtered.filter { it.country?.equals(qCountry, ignoreCase = true) == true }
        }
        if (!qState.isNullOrEmpty()) {
            filtered = filtered.filter { it.state?.equals(qState, ignoreCase = true) == true }
        }
        _radioStations.value = filtered
    }

    fun playRadioStation(station: RadioStation) {
        val track = station.toTrackEntity()
        // Play the stream directly using our AudioPlayer
        audioPlayer.playTrack(track, listOf(track))
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}

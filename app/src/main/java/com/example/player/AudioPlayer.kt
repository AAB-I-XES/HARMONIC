package com.example.player

import android.content.Context
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.TrackEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlayer(private val context: Context) : Player.Listener {
    private val TAG = "AudioPlayer"
    var exoPlayer: ExoPlayer? = null
        private set

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private var notificationManager: MediaNotificationManager? = null

    private val _currentTrack = MutableStateFlow<TrackEntity?>(null)
    val currentTrack: StateFlow<TrackEntity?> = _currentTrack.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Playlist Queue
    private val _queue = MutableStateFlow<List<TrackEntity>>(emptyList())
    val queue: StateFlow<List<TrackEntity>> = _queue.asStateFlow()

    private var currentIndex = -1
    private var progressJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        initPlayer()
        notificationManager = MediaNotificationManager(context, this)
    }

    private fun initPlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context)
                .build().apply {
                    addListener(this@AudioPlayer)
                }
        }
    }

    fun playTrack(track: TrackEntity, playlistQueue: List<TrackEntity> = listOf(track)) {
        initPlayer()
        _queue.value = playlistQueue
        currentIndex = playlistQueue.indexOfFirst { it.id == track.id }
        if (currentIndex == -1) {
            _queue.value = listOf(track) + playlistQueue
            currentIndex = 0
        }
        
        setupPlayerAndPlay(track)
    }

    private fun setupPlayerAndPlay(track: TrackEntity) {
        val player = exoPlayer ?: return
        _currentTrack.value = track
        
        val url = track.getPlayableUrl()
        Log.d(TAG, "Playing URL/Path: $url")
        
        player.stop()
        player.clearMediaItems()
        
        // Build playable object
        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaId(track.id)
            .build()
            
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
        
        startProgressTracker()
        updateNotification()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
    }

    private fun updateNotification() {
        val track = _currentTrack.value
        val playing = _isPlaying.value
        if (track != null) {
            notificationManager?.showNotification(track, playing)
        } else {
            notificationManager?.dismissNotification()
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                _currentTrack.value?.let { setupPlayerAndPlay(it) }
            } else {
                player.play()
            }
        }
    }

    fun next() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_isRepeat.value) {
            setupPlayerAndPlay(q[currentIndex])
            return
        }

        if (_isShuffle.value) {
            if (q.size > 1) {
                var nextIdx = currentIndex
                while (nextIdx == currentIndex) {
                    nextIdx = (0..q.lastIndex).random()
                }
                currentIndex = nextIdx
            }
            setupPlayerAndPlay(q[currentIndex])
        } else {
            if (currentIndex in 0 until q.lastIndex) {
                currentIndex++
                setupPlayerAndPlay(q[currentIndex])
            } else {
                currentIndex = 0
                setupPlayerAndPlay(q[0])
            }
        }
    }

    fun previous() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (_isRepeat.value) {
            seekTo(0)
            return
        }

        if (exoPlayer != null && exoPlayer!!.currentPosition > 3000) {
            seekTo(0)
            return
        }

        if (_isShuffle.value) {
            if (q.size > 1) {
                var prevIdx = currentIndex
                while (prevIdx == currentIndex) {
                    prevIdx = (0..q.lastIndex).random()
                }
                currentIndex = prevIdx
            }
            setupPlayerAndPlay(q[currentIndex])
        } else {
            if (currentIndex > 0) {
                currentIndex--
                setupPlayerAndPlay(q[currentIndex])
            } else {
                currentIndex = q.lastIndex
                setupPlayerAndPlay(q[currentIndex])
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = playerScope.launch {
            while (isActive) {
                exoPlayer?.let {
                    if (it.isPlaying) {
                        _currentPosition.value = it.currentPosition
                        _duration.value = it.duration.coerceAtLeast(0L)
                    }
                }
                delay(250) // High frequency ticks for extremely smooth slide graphics
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    // Media3 Player Listeners
    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
        if (isPlaying) {
            startProgressTracker()
        } else {
            stopProgressTracker()
        }
        updateNotification()
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_READY -> {
                _duration.value = exoPlayer?.duration?.coerceAtLeast(0L) ?: 0L
            }
            Player.STATE_ENDED -> {
                next() // Auto advance queue!
            }
            Player.STATE_BUFFERING -> {}
            Player.STATE_IDLE -> {}
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        Log.e(TAG, "ExoPlayer playback error: ${error.message} (code ${error.errorCode})")
        // If local file failed for some reason, try online streaming as a safe fallback mechanism!
        val current = _currentTrack.value
        if (current != null && current.isDownloaded) {
            Log.d(TAG, "Local file error. Attempting online fallback uri stream...")
            val fallbackTrack = current.copy(isDownloaded = false)
            _currentTrack.value = fallbackTrack
            setupPlayerAndPlay(fallbackTrack)
        } else {
            // Advancing queue as absolute failsafe
            next()
        }
    }

    fun release() {
        stopProgressTracker()
        playerScope.cancel()
        exoPlayer?.release()
        exoPlayer = null
        notificationManager?.dismissNotification()
        notificationManager?.unregisterReceiver()
    }
}

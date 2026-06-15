package com.example.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.MusicViewModel
import java.util.Locale

@Composable
fun SkipPreviousVector(modifier: Modifier = Modifier, color: Color = Color.White) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(18.dp).background(color, RoundedCornerShape(1.dp)))
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp).rotate(180f)
        )
    }
}

@Composable
fun SkipNextVector(modifier: Modifier = Modifier, color: Color = Color.White) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Box(modifier = Modifier.width(4.dp).height(18.dp).background(color, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onMinimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by viewModel.audioPlayer.currentTrack.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    val progressMs by viewModel.audioPlayer.currentPosition.collectAsState()
    val durationMs by viewModel.audioPlayer.duration.collectAsState()
    val isShuffle by viewModel.audioPlayer.isShuffle.collectAsState()
    val isRepeat by viewModel.audioPlayer.isRepeat.collectAsState()

    val track = currentTrack ?: return

    // Dynamic scale and depth animation based on whether player is playing
    val albumCoverSize by animateDpAsState(
        targetValue = if (isPlaying) 280.dp else 230.dp,
        animationSpec = tween(durationMillis = 400),
        label = "albumScale"
    )
    val playPauseScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.15f else 1.0f,
        label = "playPause"
    )

    // Formatter helpers for MM:SS
    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }

    // Dynamic backing gradient block: 100% opaque, organic, high density ambient gradient
    val dynamicBrush = remember(track.id) {
        val hash = track.title.hashCode()
        // Extract stable values using absolute value math
        val absHash = kotlin.math.abs(hash)
        val r = (absHash % 70) + 15
        val g = ((absHash / 100) % 70) + 10
        val b = ((absHash / 10000) % 80) + 25
        val c1 = Color(r, g, b, 255) // 100% fully opaque ambient primary color
        val c2 = Color(0xFF100222) // Pure solid Luxury dark base
        Brush.verticalGradient(
            colors = listOf(c1, c2)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(dynamicBrush)
            .padding(top = 28.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
            .testTag("expanded_player")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header minimize indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMinimizeClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = "NOW PLAYING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                
                // Active local source badge indicator
                Box(
                    modifier = Modifier
                        .background(
                            if (track.isDownloaded) Color(0xFF10B981) else Color(0xFF6750A4),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (track.isDownloaded) "OFFLINE" else "ONLINE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Central Album Artwork displaying elastic scaling
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.size(albumCoverSize),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                ) {
                    AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Track details and action pills representation
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = track.artistName,
                            fontSize = 17.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    // Quick add to playlist visual button
                    IconButton(
                        onClick = { viewModel.addTrackToPlaylist("Vibe Lounge", track) },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.12f), CircleShape)
                            .testTag("add_to_vibe_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Save track to Vibe list",
                            tint = Color.White
                        )
                    }
                }

                // Slider seek controls
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = progressMs.toFloat(),
                        onValueChange = { viewModel.audioPlayer.seekTo(it.toLong()) },
                        valueRange = 0f..(durationMs.toFloat().coerceAtLeast(1f)),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("player_seek_slider")
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(progressMs),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = formatTime(durationMs),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // Player Controls (Drawn beautifully!)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle Toggle Button
                    IconButton(
                        onClick = { viewModel.audioPlayer.toggleShuffle() },
                        modifier = Modifier.size(48.dp).testTag("shuffle_toggle")
                    ) {
                        Text(
                            text = "🔀",
                            fontSize = 24.sp,
                            color = if (isShuffle) Color(0xFFFA243C) else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // Previous Button
                    IconButton(
                        onClick = { viewModel.audioPlayer.previous() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        SkipPreviousVector(modifier = Modifier.size(36.dp))
                    }

                    // Main Play Toggle Button
                    Box(
                        modifier = Modifier
                            .scale(playPauseScale)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { viewModel.audioPlayer.togglePlayPause() }
                            .testTag("playing_toggle_pivoted"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying) {
                            PauseVector(modifier = Modifier.fillMaxHeight(0.36f), color = Color.Black)
                        } else {
                            PlayVector(modifier = Modifier.size(36.dp), color = Color.Black)
                        }
                    }

                    // Next Button
                    IconButton(
                        onClick = { viewModel.audioPlayer.next() },
                        modifier = Modifier.size(56.dp)
                    ) {
                        SkipNextVector(modifier = Modifier.size(36.dp))
                    }

                    // Repeat Toggle Button
                    IconButton(
                        onClick = { viewModel.audioPlayer.toggleRepeat() },
                        modifier = Modifier.size(48.dp).testTag("repeat_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Repeat",
                            tint = if (isRepeat) Color(0xFFFA243C) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Sub-footer scrolling mock lyricist block (Dynamic Apple Music flair!)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0EFFFFFF))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "lyrics",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "Lyrics are loaded dynamically in live player...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "♪ Let the frequencies carry your thoughts away into the clouds... ♪",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

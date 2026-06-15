package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.PlaylistEntity
import com.example.ui.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    viewModel: MusicViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val playlistTracks by viewModel.activePlaylistTracks.collectAsState()

    val currentPlaylist = activePlaylist ?: return

    val dynamicBrush = remember(currentPlaylist.accentColor) {
        val baseColor = try {
            Color(android.graphics.Color.parseColor(currentPlaylist.accentColor ?: "#6750A4"))
        } catch (e: Exception) {
            Color(0xFF6750A4)
        }
        Brush.verticalGradient(
            colors = listOf(baseColor.copy(alpha = 0.35f), Color(0xFFFEF7FF)) // light theme blend
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1D1B20))
                    }
                },
                actions = {
                    // Can delete custom playlists
                    IconButton(
                        onClick = {
                            viewModel.deletePlaylist(currentPlaylist.id)
                            onBackClick()
                        },
                        modifier = Modifier.testTag("delete_playlist_button")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color(0xFF49454F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color(0xFF1D1B20)
                )
            )
        },
        containerColor = Color(0xFFFEF7FF),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(dynamicBrush)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card Displaying Cover Art and dynamic description block
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        AsyncImage(
                            model = currentPlaylist.coverUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80",
                            contentDescription = currentPlaylist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        text = currentPlaylist.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1D1B20),
                        textAlign = TextAlign.Center
                    )

                    if (!currentPlaylist.description.isNullOrEmpty()) {
                        Text(
                            text = currentPlaylist.description,
                            fontSize = 12.sp,
                            color = Color(0xFF49454F),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }

                    // Large quick play playlist button
                    Button(
                        onClick = {
                            if (playlistTracks.isNotEmpty()) {
                                viewModel.playTrack(playlistTracks.first(), playlistTracks)
                            }
                        },
                        enabled = playlistTracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6750A4),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFCAC4D0)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                        modifier = Modifier.testTag("playlist_play_full_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "PlayAll", tint = Color.White)
                            Text("Play", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Playlist tracks section
            item {
                Text(
                    text = "Tracks (${playlistTracks.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            if (playlistTracks.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("This playlist has no tracks yet.", color = Color(0xFF49454F))
                        Text(
                            text = "Tip: Search for songs under 'Online Mode' and tap download or play, they will appear or can be added directly which persists instantly to database.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F).copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                items(playlistTracks) { track ->
                    // Wrap track item row that lets them play, or delete from playlist.
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playTrack(track, playlistTracks) }
                            .testTag("playlist_track_row_${track.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cover art
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(6.dp))
                            )

                            // Titles
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1D1B20),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artistName,
                                    fontSize = 11.sp,
                                    color = Color(0xFF49454F),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Delete/Remove button linking database
                            IconButton(
                                onClick = { viewModel.removeTrackFromPlaylist(currentPlaylist.id, track.id) },
                                modifier = Modifier.testTag("remove_track_${track.id}_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove from playlist",
                                    tint = Color(0xFF49454F).copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

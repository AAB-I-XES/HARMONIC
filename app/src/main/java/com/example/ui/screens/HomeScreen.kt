package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import coil.compose.AsyncImage
import com.example.data.PlaylistEntity
import com.example.data.TrackEntity
import com.example.ui.MusicViewModel

// Core custom symbols for safe rendering
@Composable
fun PlayVector(modifier: Modifier = Modifier, color: Color = Color.White) {
    Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Play",
        tint = color,
        modifier = modifier
    )
}

@Composable
fun PauseVector(modifier: Modifier = Modifier, color: Color = Color.White) {
    // Custom drew Pause double bars to guarantee no missing symbol issue
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(color, RoundedCornerShape(1.dp)))
        Box(modifier = Modifier.width(5.dp).fillMaxHeight().background(color, RoundedCornerShape(1.dp)))
    }
}

@Composable
fun DownloadedIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(Color(0xFF10B981), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Offline cached",
            tint = Color.White,
            modifier = Modifier.size(10.dp)
        )
    }
}

enum class MainTab {
    HOME,
    PLAYLISTS,
    LIBRARY,
    PROFILE
}

fun safeColorParse(hex: String, fallbackColorHex: String = "#6750A4"): Color {
    return try {
        val cleanHex = hex.trim().removePrefix("#")
        if (cleanHex.length == 6) {
            Color(cleanHex.toLong(16) or 0xFF000000)
        } else if (cleanHex.length == 8) {
            Color(cleanHex.toLong(16))
        } else {
            Color(android.graphics.Color.parseColor(hex))
        }
    } catch (e: Exception) {
        try {
            val fallbackClean = fallbackColorHex.trim().removePrefix("#")
            if (fallbackClean.length == 6) {
                Color(fallbackClean.toLong(16) or 0xFF000000)
            } else {
                Color(0xFF6750A4)
            }
        } catch (ex: Exception) {
            Color(0xFF6750A4)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onPlaylistClick: (PlaylistEntity) -> Unit,
    onAuthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trendingTracks by viewModel.onlineTrendingTracks.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoadingSongs by viewModel.isLoadingSongs.collectAsState()
    val playlists by viewModel.playlistsFlow.collectAsState()
    val downloadedTracks by viewModel.downloadedTracksFlow.collectAsState()
    val isSmartCurating by viewModel.isSmartCurating.collectAsState()
    val forceOfflineMode by viewModel.forceOfflineMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    
    val focusManager = LocalFocusManager.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    var smartPromptInput by remember { mutableStateOf("") }

    // Multi-tab active controller
    var activeTab by remember { mutableStateOf(MainTab.HOME) }
    
    // Aesthetic customizations
    var profileNickname by remember { mutableStateOf("Sarah") }
    var isEditingNickname by remember { mutableStateOf(false) }
    var selectedAccentColor by remember { mutableStateOf("#6750A4") } // Premium signature primary
    var selectedHeaderNav by remember { mutableStateOf("All") }

    val isOnline = viewModel.isNetworkAvailable()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFEF7FF)), // High Density Light BG
        contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // High-fidelity App Header and nav row selector
        item {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Navigation pills: All, Music, Radio
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("All", "Music", "Radio").forEach { nav ->
                            val isSelected = selectedHeaderNav == nav
                            val activeColor = safeColorParse(selectedAccentColor)
                            Box(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor else Color(0xFFF3EDF7))
                                    .clickable { selectedHeaderNav = nav }
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = nav,
                                    color = if (isSelected) Color.White else Color(0xFF49454F),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Profile picture icon on the right corner
                    AsyncImage(
                        model = currentUser?.avatarUrl ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&q=80",
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onAuthClick() }
                            .testTag("profile_picture_icon")
                    )
                }

                // Apple Music styled segmented tab selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3EDF7))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MainTab.values().forEach { tab ->
                        val isActive = activeTab == tab
                        val activeColor = safeColorParse(selectedAccentColor)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) activeColor else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (tab) {
                                    MainTab.HOME -> "Home"
                                    MainTab.PLAYLISTS -> "Playlists"
                                    MainTab.LIBRARY -> "Library"
                                    MainTab.PROFILE -> "Profile"
                                },
                                color = if (isActive) Color.White else Color(0xFF49454F),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // ======================== TAB RENDERING ========================

        // --- TAB 1: HOME ---
        if (activeTab == MainTab.HOME) {
            // Search field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchSongs(it) },
                    placeholder = { Text("Search songs, artists, moods...", color = Color(0xFF49454F), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = safeColorParse(selectedAccentColor)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchSongs("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF49454F))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF3EDF7),
                        unfocusedContainerColor = Color(0xFFF3EDF7),
                        focusedBorderColor = safeColorParse(selectedAccentColor),
                        unfocusedBorderColor = Color(0xFFCAC4D0),
                        focusedTextColor = Color(0xFF1D1B20),
                        unfocusedTextColor = Color(0xFF1D1B20)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field"),
                    singleLine = true
                )
            }

            // Search results display
            if (searchQuery.isNotEmpty()) {
                item {
                    Text(
                        text = "Search Results",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                }

                if (isLoadingSongs) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = safeColorParse(selectedAccentColor))
                        }
                    }
                } else if (searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No songs found matching query", color = Color(0xFF49454F), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(searchResults) { track ->
                        TrackRowItem(
                          track = track,
                          onPlayClick = { viewModel.playTrack(track, searchResults) },
                          onDownloadClick = { viewModel.downloadTrack(track) },
                          onDeleteClick = { viewModel.removeTrackDownload(track) },
                          viewModel = viewModel
                        )
                    }
                }
            } else {
                // Discover Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            safeColorParse(selectedAccentColor),
                                            Color(0xFF21005D)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("WEEKLY DISCOVERY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text(
                                    text = "Explore Atmospheric Feeds",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    lineHeight = 28.sp
                                )
                                Text(
                                    text = "Tap any song to play. High fidelity sound vectors are loaded seamlessly to your device context.",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Trending Songs Section
                item {
                    Text(
                        text = if (isOnline) "Featured Music Streams" else "Curated Music (Offline fallback)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                }

                if (isLoadingSongs && trendingTracks.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = safeColorParse(selectedAccentColor))
                        }
                    }
                } else {
                    items(trendingTracks) { track ->
                        TrackRowItem(
                            track = track,
                            onPlayClick = { viewModel.playTrack(track, trendingTracks) },
                            onDownloadClick = { viewModel.downloadTrack(track) },
                            onDeleteClick = { viewModel.removeTrackDownload(track) },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // --- TAB 2: PLAYLISTS ---
        if (activeTab == MainTab.PLAYLISTS) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Playlists",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = "${playlists.size} playlists active",
                            fontSize = 12.sp,
                            color = Color(0xFF49454F)
                        )
                    }
                    
                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = safeColorParse(selectedAccentColor)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No playlists created yet. Start by making one!", color = Color(0xFF49454F))
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists) { playlist ->
                            PlaylistCard(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist) }
                            )
                        }
                    }
                }
            }

            // Smart Companion Curator Block
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_generator_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        safeColorParse(selectedAccentColor),
                                        Color(0xFFD0BCFF)
                                    )
                                )
                            )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                        contentAlignment = Alignment.Center
                                      ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Smart",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Smart Muse Companion",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            
                            Text(
                                text = "Briefly type your mood and the smart curator will instantly build a personalized board with dynamic colors, matched poems, and fitting soundtracks.",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = smartPromptInput,
                                    onValueChange = { smartPromptInput = it },
                                    placeholder = { Text("E.g., Rain meditation mood...", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.White.copy(alpha = 0.15f),
                                        unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.generateSmartPersonalizedPlaylist(smartPromptInput) { id ->
                                            smartPromptInput = ""
                                        }
                                    },
                                    enabled = smartPromptInput.isNotBlank() && !isSmartCurating,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = safeColorParse(selectedAccentColor),
                                        disabledContainerColor = Color.White.copy(alpha = 0.4f),
                                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp),
                                    modifier = Modifier.testTag("smart_generate_button")
                                ) {
                                    if (isSmartCurating) {
                                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Text("Compile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            
                            // Visual prompt suggestions
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Lofi Chill", "Neon Drive", "Focus Forest").forEach { cue ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.15f))
                                            .clickable { smartPromptInput = cue }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(cue, fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- TAB 3: LIBRARY (Downloaded Offline Tracks) ---
        if (activeTab == MainTab.LIBRARY) {
            item {
                Column {
                    Text(
                        text = "Your Library",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = "Offline local audio compilation cache",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F)
                    )
                }
            }

            item {
                // High density details info cards of active memory
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${downloadedTracks.size}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = safeColorParse(selectedAccentColor))
                            Text(text = "Tracks", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFCAC4D0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val megabytes = downloadedTracks.size * 4.4
                            Text(text = String.format(Locale.getDefault(), "%.1f MB", megabytes), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1D1B20))
                            Text(text = "Disk Cache", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFCAC4D0)))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isOnline) "Ready" else "Offline", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isOnline) Color(0xFF34C759) else Color(0xFFFA243C))
                            Text(text = "Engine State", fontSize = 11.sp, color = Color(0xFF49454F))
                        }
                    }
                }
            }

            if (downloadedTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFF3EDF7), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Info, contentDescription = "Empty", tint = Color(0xFF49454F).copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                            Text(
                                text = "Library is empty.",
                                color = Color(0xFF1D1B20),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Search online tracks in 'Home' tab and tap the download icon to save them offline!",
                                color = Color(0xFF49454F),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            } else {
                items(downloadedTracks) { track ->
                    TrackRowItem(
                        track = track,
                        onPlayClick = { viewModel.playTrack(track, downloadedTracks) },
                        onDownloadClick = {}, // ALREADY LOADED
                        onDeleteClick = { viewModel.removeTrackDownload(track) },
                        viewModel = viewModel
                    )
                }
            }
        }

        // --- TAB 4: PROFILE ---
        if (activeTab == MainTab.PROFILE) {
            item {
                Column {
                    Text(
                        text = "CONTRIBUTOR ACCOUNT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = safeColorParse(selectedAccentColor),
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Your Profile",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                }
            }

            // Beautiful profile card with interactive account values and nickname editing options
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("profile_status_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                safeColorParse(selectedAccentColor),
                                                Color(0xFF1D1B20)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (currentUser != null) "✨" else "🎧", fontSize = 32.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                if (currentUser != null) {
                                    val currentProfile = currentUser!!
                                    if (isEditingNickname) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = profileNickname,
                                                onValueChange = { profileNickname = it },
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedContainerColor = Color.White,
                                                    unfocusedContainerColor = Color.White,
                                                    focusedBorderColor = safeColorParse(selectedAccentColor)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).height(48.dp),
                                                singleLine = true
                                            )
                                            IconButton(
                                                onClick = { 
                                                    isEditingNickname = false 
                                                    viewModel.loginUser(currentProfile.email, profileNickname)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color(0xFF34C759))
                                            }
                                        }
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = currentProfile.username,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1D1B20)
                                            )
                                            IconButton(
                                                onClick = { 
                                                    profileNickname = currentProfile.username
                                                    isEditingNickname = true 
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit name", tint = Color(0xFF49454F), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    
                                    Text(
                                        text = currentProfile.email,
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F)
                                    )
                                } else {
                                    Text(
                                        text = "Guest Listener",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF1D1B20)
                                    )
                                    Text(
                                        text = "Not logged in",
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFCAC4D0))

                        // High density stats indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("38.4 hrs", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                Text("Listening time", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            Column {
                                Text("Synthwave", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                                Text("Primary Genre", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("12 Days", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFA243C))
                                    Text("🔥", fontSize = 14.sp)
                                }
                                Text("Active Streak", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                        }

                        HorizontalDivider(color = Color(0xFFCAC4D0))

                        if (currentUser != null) {
                            Button(
                                onClick = { viewModel.logoutUser() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFFFA243C)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("profile_logout_button")
                            ) {
                                Text("Log Out Account", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = onAuthClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = safeColorParse(selectedAccentColor),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().testTag("profile_login_button")
                            ) {
                                Text("Log In or Sign Up", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Theme visual color customizations
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Primary Accent Palette",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D1B20)
                        )
                        Text(
                            text = "Modify active accents applied to key elements across the tabs in real time.",
                            fontSize = 11.sp,
                            color = Color(0xFF49454F)
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            val accentPalettes = listOf(
                                "#6750A4" to "Classic Purple",
                                "#FA243C" to "Apple Red",
                                "#10B981" to "Emerald Mint",
                                "#D946EF" to "Violet Glow"
                            )
                            accentPalettes.forEach { (hex, name) ->
                                val isSelected = selectedAccentColor == hex
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(safeColorParse(hex))
                                        .clickable { selectedAccentColor = hex }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active color", tint = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text(name.split(" ")[1], fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // App simulation toggles
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = "App Environment Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1D1B20))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Simulated Offline Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D1B20))
                                Text("Forces application to run off downloaded and database cached files only.", fontSize = 10.sp, color = Color(0xFF49454F))
                            }
                            Switch(
                                checked = forceOfflineMode,
                                onCheckedChange = { viewModel.toggleForceOfflineMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(android.graphics.Color.parseColor(selectedAccentColor))
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal popup to build customized playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("New Playlist", color = Color(0xFF1D1B20), fontWeight = FontWeight.Bold) },
            containerColor = Color(0xFFFEF7FF),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name", color = Color(0xFF49454F)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedContainerColor = Color(0xFFF3EDF7),
                            unfocusedContainerColor = Color(0xFFF3EDF7),
                            focusedBorderColor = Color(0xFF6750A4)
                        )
                    )
                    OutlinedTextField(
                        value = newPlaylistDesc,
                        onValueChange = { newPlaylistDesc = it },
                        label = { Text("Description (Optional)", color = Color(0xFF49454F)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1D1B20),
                            unfocusedTextColor = Color(0xFF1D1B20),
                            focusedContainerColor = Color(0xFFF3EDF7),
                            unfocusedContainerColor = Color(0xFFF3EDF7),
                            focusedBorderColor = Color(0xFF6750A4)
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                    Text("Cancel", color = Color(0xFF49454F))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createCustomPlaylist(newPlaylistName, newPlaylistDesc)
                            newPlaylistName = ""
                            newPlaylistDesc = ""
                            showCreatePlaylistDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                ) {
                    Text("Create")
                }
            }
        )
    }
}

@Composable
fun PlaylistCard(
    playlist: PlaylistEntity,
    onClick: () -> Unit
) {
    val dynamicBrush = remember(playlist.accentColor) {
        val baseColor = safeColorParse(playlist.accentColor ?: "#6750A4")
        Brush.linearGradient(
            colors = listOf(baseColor, baseColor.copy(alpha = 0.4f))
        )
    }

    Box(
        modifier = Modifier
            .size(width = 160.dp, height = 160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(dynamicBrush)
            .clickable { onClick() }
            .padding(16.dp)
            .testTag("playlist_card_${playlist.name.replace(" ", "_")}")
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Render custom Apple style glassy layout
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0x3F000000), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playlist.isSmartGenerated) Icons.Default.Star else Icons.Default.List,
                        contentDescription = "Theme style",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                if (playlist.isSmartGenerated) {
                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Smart", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            Column {
                Text(
                    text = playlist.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (!playlist.description.isNullOrEmpty()) {
                    Text(
                        text = playlist.description,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun TrackRowItem(
    track: TrackEntity,
    onPlayClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    viewModel: MusicViewModel
) {
    val progress = viewModel.downloadProgressMap[track.id]
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("track_row_${track.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album artwork with dynamic fallback
            Box(modifier = Modifier.size(56.dp)) {
                AsyncImage(
                    model = track.coverUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                
                // Play overlay triggers on card tap or play click
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x2F000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0x9F000000), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        PlayVector(modifier = Modifier.size(14.dp))
                    }
                }

                // If downloaded, overlay checking badge
                if (track.isDownloaded) {
                    DownloadedIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(4.dp, (-4).dp)
                    )
                }
            }

            // Metadata text blocks
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D1B20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artistName,
                    fontSize = 12.sp,
                    color = Color(0xFF49454F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Downloader / cache deletion trigger
            Box(contentAlignment = Alignment.Center) {
                when {
                    progress != null -> {
                        // Circular animation indicator of progress bytes
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                            CircularProgressIndicator(
                                progress = { progress },
                                color = Color(0xFF6750A4),
                                strokeWidth = 3.dp,
                                modifier = Modifier.fillMaxSize()
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1D1B20)
                            )
                        }
                    }
                    track.isDownloaded -> {
                        // Trash offline files icon
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove download",
                                tint = Color(0xFF49454F).copy(alpha = 0.6f)
                            )
                        }
                    }
                    else -> {
                        // Action triggers network cache retrieve
                        IconButton(onClick = onDownloadClick) {
                            Icon(
                                imageVector = Icons.Default.Star, // standard core representation for downloading
                                contentDescription = "Download song",
                                tint = Color(0xFF6750A4)
                            )
                        }
                    }
                }
            }
        }
    }
}

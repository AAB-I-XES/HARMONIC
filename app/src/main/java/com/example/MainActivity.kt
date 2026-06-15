package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.MusicViewModel
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PauseVector
import com.example.ui.screens.PlayVector
import com.example.ui.screens.PlayerScreen
import com.example.ui.screens.PlaylistScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.MyApplicationTheme

enum class MusicScreen {
    HOME,
    PLAYLIST,
    AUTH
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request post notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            MyApplicationTheme(darkTheme = isDarkMode) {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: MusicViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(MusicScreen.HOME) }
    var isExpandedPlayerVisible by remember { mutableStateOf(false) }
    
    // Playback state subscription
    val currentTrack by viewModel.audioPlayer.currentTrack.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Core View Switcher Frame
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    MusicScreen.HOME -> {
                        HomeScreen(
                            viewModel = viewModel,
                            onPlaylistClick = { playlist ->
                                viewModel.viewPlaylist(playlist)
                                currentScreen = MusicScreen.PLAYLIST
                            },
                            onAuthClick = {
                                currentScreen = MusicScreen.AUTH
                            }
                        )
                    }
                    MusicScreen.PLAYLIST -> {
                        PlaylistScreen(
                            viewModel = viewModel,
                            onBackClick = {
                                currentScreen = MusicScreen.HOME
                            }
                        )
                    }
                    MusicScreen.AUTH -> {
                        AuthScreen(
                            viewModel = viewModel,
                            onBackClick = {
                                currentScreen = MusicScreen.HOME
                            }
                        )
                    }
                }
            }
        }

        // Float-overlay Bottom Mini Player Bar
        AnimatedVisibility(
            visible = currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
        ) {
            currentTrack?.let { track ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isExpandedPlayerVisible = true }
                        .testTag("mini_player_bar"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF21005D)), // High Density Luxury Violet Bar
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Tiny rounding cover thumbnail
                        AsyncImage(
                            model = track.coverUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )

                        // Meta details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.artistName,
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Quick play toggle
                            IconButton(
                                onClick = { viewModel.audioPlayer.togglePlayPause() },
                                modifier = Modifier.testTag("mini_play_toggle")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPlaying) {
                                        PauseVector(modifier = Modifier.height(12.dp), color = Color.White)
                                    } else {
                                        PlayVector(modifier = Modifier.size(20.dp), color = Color.White)
                                    }
                                }
                            }

                            // Advance skip toggle
                            IconButton(onClick = { viewModel.audioPlayer.next() }) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            PlayVector(modifier = Modifier.size(12.dp), color = Color.White)
                                            Box(modifier = Modifier.size(width = 2.dp, height = 10.dp).background(Color.White))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Expanding Presentation Card Player Overlay
        AnimatedVisibility(
            visible = isExpandedPlayerVisible,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                viewModel = viewModel,
                onMinimizeClick = { isExpandedPlayerVisible = false }
            )
        }
    }
}

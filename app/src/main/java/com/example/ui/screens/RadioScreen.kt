package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.RadioStation
import com.example.ui.MusicViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RadioScreen(
    viewModel: MusicViewModel,
    selectedAccentColor: String,
    modifier: Modifier = Modifier
) {
    val radioStations by viewModel.radioStations.collectAsState()
    val isSearchingRadio by viewModel.isSearchingRadio.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var countryInput by remember { mutableStateOf("") }
    var stateInput by remember { mutableStateOf("") }
    
    val activeTrack by viewModel.audioPlayer.currentTrack.collectAsState()
    val isPlaying by viewModel.audioPlayer.isPlaying.collectAsState()
    
    val focusManager = LocalFocusManager.current
    val accentColor = safeColorParse(selectedAccentColor)
    val adaptiveBgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFFEF7FF)
    val adaptiveContainerBgColor = if (isDarkMode) Color(0xFF1C1B1F) else Color(0xFFF3EDF7)

    // Triggers initial fetch of top radio stations
    LaunchedEffect(Unit) {
        if (radioStations.isEmpty()) {
            viewModel.fetchRadioStations()
        }
    }

    val popularCountries = listOf(
        "All" to "All countries",
        "US" to "United States",
        "GB" to "United Kingdom",
        "IN" to "India",
        "DE" to "Germany",
        "FR" to "France",
        "ES" to "Spain",
        "BR" to "Brazil",
        "CA" to "Canada"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(adaptiveBgColor)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Advanced filter search console
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = adaptiveContainerBgColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Search & Filter Broadcasts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 0.5.sp
                )

                // Station query name
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or keyword...", fontSize = 13.sp) },
                    prefix = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF49454F)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.updateRadioFilters(searchQuery, countryInput, stateInput)
                        focusManager.clearFocus()
                    })
                )

                // Row of Country & State text-inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = countryInput,
                        onValueChange = { countryInput = it },
                        placeholder = { Text("Country (e.g., India)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(50.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.updateRadioFilters(searchQuery, countryInput, stateInput)
                            focusManager.clearFocus()
                        })
                    )

                    OutlinedTextField(
                        value = stateInput,
                        onValueChange = { stateInput = it },
                        placeholder = { Text("State (e.g., California)", fontSize = 12.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(50.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            viewModel.updateRadioFilters(searchQuery, countryInput, stateInput)
                            focusManager.clearFocus()
                        })
                    )
                }

                // Search Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset Button
                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            countryInput = ""
                            stateInput = ""
                            viewModel.updateRadioFilters("", "", "")
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF49454F)),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Reset Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Search Button
                    Button(
                        onClick = {
                            viewModel.updateRadioFilters(searchQuery, countryInput, stateInput)
                            focusManager.clearFocus()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        modifier = Modifier.weight(1.2f).height(38.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apply Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Quick Pick Countries Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(popularCountries) { pair ->
                val code = pair.first
                val fullName = pair.second
                val isSelected = (fullName == countryInput) || (code == "All" && countryInput.isEmpty())
                val itemBg = if (isSelected) accentColor else adaptiveContainerBgColor
                val itemText = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color(0xFF49454F)

                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .background(itemBg)
                        .clickable {
                            if (code == "All") {
                                countryInput = ""
                            } else {
                                countryInput = fullName
                            }
                            viewModel.updateRadioFilters(searchQuery, countryInput, stateInput)
                        }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (code != "All") {
                            Text(getCountryFlagEmoji(code), fontSize = 14.sp)
                        }
                        Text(
                            text = if (code == "All") "Global Top" else code,
                            color = itemText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Header / Loading state
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (countryInput.isNotBlank()) "Stations in $countryInput" else "Popular Channels",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1D1B20)
            )

            if (isSearchingRadio) {
                CircularProgressIndicator(
                    color = accentColor,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Text(
                    text = "${radioStations.size} found",
                    fontSize = 11.sp,
                    color = Color(0xFF49454F)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Stations List
        if (radioStations.isEmpty() && !isSearchingRadio) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning, // WiFi offline style fallback
                        contentDescription = null,
                        tint = Color(0xFFCAC4D0),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No Stations Found",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D1B20)
                    )
                    Text(
                        text = "Try clearing filters, searching different states or checking connection.",
                        fontSize = 12.sp,
                        color = Color(0xFF49454F),
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(radioStations, key = { it.stationuuid }) { station ->
                    val isCurrentRadioPlaying = (activeTrack?.id == "radio_${station.stationuuid}")
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = if (isCurrentRadioPlaying) 2.dp else 0.dp,
                                color = if (isCurrentRadioPlaying) accentColor else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.playRadioStation(station)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentRadioPlaying) {
                                accentColor.copy(alpha = 0.08f)
                            } else {
                                Color.White
                            }
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Cover art / icon with floating visual audio waves
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(adaptiveContainerBgColor),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = if (!station.favicon.isNullOrBlank() && station.favicon.startsWith("http")) {
                                        station.favicon
                                    } else {
                                        "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=150&q=80"
                                    },
                                    contentDescription = station.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay active equalizer if playing
                                if (isCurrentRadioPlaying && isPlaying) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnimatedEqualizerBars(color = Color.White)
                                    }
                                }
                            }

                            // Meta info details
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = station.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D1B20),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    
                                    if (isCurrentRadioPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .background(accentColor, CircleShape)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "ON AIR",
                                                color = Color.White,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                // Location String State / Country
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = Color(0xFF49454F),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = buildString {
                                            if (!station.state.isNullOrBlank()) {
                                                append(station.state)
                                                if (!station.country.isNullOrBlank()) append(", ")
                                            }
                                            if (!station.country.isNullOrBlank()) {
                                                append(station.country)
                                            }
                                            if (isEmpty()) append("International Feed")
                                        },
                                        fontSize = 11.sp,
                                        color = Color(0xFF49454F),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Genres Tags line
                                if (!station.tags.isNullOrBlank()) {
                                    Text(
                                        text = station.tags.split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                            .take(3)
                                            .joinToString(" • "),
                                        fontSize = 10.sp,
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Interactive stream trigger controls
                            IconButton(
                                onClick = {
                                    viewModel.playRadioStation(station)
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isCurrentRadioPlaying) accentColor else adaptiveContainerBgColor,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCurrentRadioPlaying && isPlaying) {
                                        // Dynamic pause lines drawn with boxes to be 100% compile-safe
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.width(3.dp).height(12.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                            Box(modifier = Modifier.width(3.dp).height(12.dp).background(Color.White, RoundedCornerShape(1.dp)))
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Stream",
                                            tint = if (isCurrentRadioPlaying) Color.White else Color(0xFF1D1B20),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Map short letters back to beautiful country flag icons representation
fun getCountryFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return "📻"
    val firstChar = countryCode[0].uppercaseChar().code - 0x41 + 0x1F1E6
    val secondChar = countryCode[1].uppercaseChar().code - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

@Composable
fun AnimatedEqualizerBars(color: Color) {
    Row(
        modifier = Modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(0.4f, 0.9f, 0.6f, 0.2f, 0.8f).forEach { scale ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(scale)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

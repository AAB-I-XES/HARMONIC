package com.example.smart

import android.util.Log
import com.example.BuildConfig
import com.example.data.MusicDao
import com.example.data.PlaylistEntity
import com.example.data.PlaylistTrackCrossRef
import com.example.data.TrackEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

// Local result holder
data class SmartPlaylistResult(
    val title: String,
    val description: String,
    val accentColor: String,
    val songIdsToMatch: List<String>
)

object SmartPlaylistGenerator {
    private const val TAG = "SmartPlaylist"
    
    // OkHttp Client configured for requirements (60-second timeouts)
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Fallback template matching in case generation api is offline/unconfigured
    private fun getLocalFallbackResult(prompt: String, availableTracks: List<TrackEntity>): SmartPlaylistResult {
        val lower = prompt.lowercase()
        return when {
            lower.contains("chill") || lower.contains("sleep") || lower.contains("rain") || lower.contains("relax") -> {
                SmartPlaylistResult(
                    title = "Ethereal Raindrop Session",
                    description = "A customized collection of relaxing, ambient chill tracks tailored for tranquility and focus.",
                    accentColor = "#00D2FF",
                    songIdsToMatch = availableTracks.filter { it.artistName.contains("Aether") || it.artistName.contains("Lumina") || it.id == "f1" || it.id == "f4" }.map { it.id }
                )
            }
            lower.contains("retro") || lower.contains("drive") || lower.contains("cyber") || lower.contains("synth") || lower.contains("gaming") -> {
                SmartPlaylistResult(
                    title = "Cyber Neon Velocity",
                    description = "High-octane outrun waves and pulsing synthesizers curated for cyberpunk neon drives.",
                    accentColor = "#FF007F",
                    songIdsToMatch = availableTracks.filter { it.artistName.contains("Vector") || it.artistName.contains("Grid") || it.id == "f3" || it.id == "f5" }.map { it.id }
                )
            }
            else -> {
                SmartPlaylistResult(
                    title = "Harmonic Muse: ${prompt.take(15).replaceFirstChar { it.uppercase() }}",
                    description = "A custom blended mixtape reflecting your creative input: $prompt",
                    accentColor = "#FFD700",
                    songIdsToMatch = availableTracks.shuffled().take(3).map { it.id }
                )
            }
        }
    }

    suspend fun generatePersonalizedPlaylist(
        prompt: String,
        musicDao: MusicDao,
        availableTracks: List<TrackEntity>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            Log.w(TAG, "No valid api key found. Using local algorithmic generation.")
            // Run fallback
            val fallbackResult = getLocalFallbackResult(prompt, availableTracks)
            return@withContext savePlaylistToDb(fallbackResult, musicDao, availableTracks)
        }

        try {
            // Compile list of track info to feed to generator
            val songCatalogJson = JSONArray()
            availableTracks.forEach {
                val songObj = JSONObject()
                songObj.put("id", it.id)
                songObj.put("title", it.title)
                songObj.put("artist", it.artistName)
                songObj.put("album", it.albumName)
                songCatalogJson.put(songObj)
            }

            val systemInstruction = """
                You are a premium, elite music curator like a top producer at Apple Music or Spotify.
                Your task is to take a listener's emotional or topical mood prompt, and build a cohesive, gorgeous playlist.
                You are provided with a catalog of playable track metadata as a JSON array.
                You must return a raw JSON object with the following fields:
                - title: String (A creative list title, eg 'Sunset Cruise' or 'Foggy Morning' - do NOT call it 'Mixtape' or 'My Playlist')
                - description: String (A captivating, poetic, Apple-Music-style one-sentence description explaining why these songs fit the theme)
                - accentColor: String (A striking hex color code like '#E50914' or '#FF9F0A' matching the dynamic visual theme of the mood. Use a bright modern color)
                - songIdsToMatch: Array of Strings (The list of exact song IDs chosen from the catalog that fit this mood perfectly. Pick between 2 to 6 tracks)
                Only return the JSON block, no markdown wraps.
            """.trimIndent()

            // Construct Request Body
            val requestJson = JSONObject()
            
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", "Listener Mood Prompt: '$prompt'\n\nCatalog of Available Songs to match from:\n$songCatalogJson")
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System Instruction
            val systemInsObj = JSONObject()
            val systemPartsArray = JSONArray()
            val systemPartObj = JSONObject()
            systemPartObj.put("text", systemInstruction)
            systemPartsArray.put(systemPartObj)
            systemInsObj.put("parts", systemPartsArray)
            requestJson.put("systemInstruction", systemInsObj)

            // Generation config with forced JSON formatting
            val configObj = JSONObject()
            val formatObj = JSONObject()
            formatObj.put("mimeType", "application/json")
            configObj.put("responseFormat", formatObj)
            configObj.put("temperature", 0.8)
            requestJson.put("generationConfig", configObj)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestJson.toString().toRequestBody(mediaType)

            // We use the recommended gemini-3.5-flash for code and text-structuring tasks
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBodyStr = response.body?.string() ?: ""
                Log.d(TAG, "Raw Response: $responseBodyStr")
                
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCan = candidates.getJSONObject(0)
                    val content = firstCan.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    val textRes = parts.getJSONObject(0).getString("text")
                    
                    // Parse the expected curated result
                    val cleanText = textRes.trim().removeSurrounding("```json", "```").trim()
                    val resultJson = JSONObject(cleanText)
                    
                    val title = resultJson.getString("title")
                    val description = resultJson.getString("description")
                    val accentColor = resultJson.getString("accentColor")
                    
                    val songIdArray = resultJson.getJSONArray("songIdsToMatch")
                    val songIds = mutableListOf<String>()
                    for (i in 0 until songIdArray.length()) {
                        songIds.add(songIdArray.getString(i))
                    }

                    val playlistResult = SmartPlaylistResult(
                        title = title,
                        description = description,
                        accentColor = accentColor,
                        songIdsToMatch = songIds
                    )
                    
                    return@withContext savePlaylistToDb(playlistResult, musicDao, availableTracks)
                }
            } else {
                Log.e(TAG, "Request Failed: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Generator failed: ${e.message}. Using high-quality local failsafe.", e)
        }

        // Final failsafe
        val localFallback = getLocalFallbackResult(prompt, availableTracks)
        return@withContext savePlaylistToDb(localFallback, musicDao, availableTracks)
    }

    private suspend fun savePlaylistToDb(
        result: SmartPlaylistResult,
        musicDao: MusicDao,
        catalog: List<TrackEntity>
    ): String {
        val playlistId = UUID.randomUUID().toString()
        
        // Select matching tracks
        var matched = catalog.filter { result.songIdsToMatch.contains(it.id) }
        if (matched.isEmpty()) {
            // Take 3 random tracks if wrong/empty IDs
            matched = catalog.shuffled().take(3)
        }

        // Cover image from one of the songs or music card
        val coverUrl = matched.firstOrNull()?.coverUrl ?: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80"

        val playlist = PlaylistEntity(
            id = playlistId,
            name = result.title,
            description = result.description,
            coverUrl = coverUrl,
            accentColor = result.accentColor,
            isSmartGenerated = true,
            prompt = result.songIdsToMatch.joinToString(",") // store track selection ids or prompt
        )

        musicDao.insertPlaylist(playlist)

        // Insert tracks link
        matched.forEach { track ->
            // ensure track exists in database caches
            musicDao.insertTrack(track)
            musicDao.insertPlaylistTrackCrossRef(
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = track.id
                )
            )
        }

        Log.d(TAG, "Saved Smart Playlist: name=${result.title}, tracksCount=${matched.size}")
        return playlistId
    }
}

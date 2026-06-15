package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class RadioStation(
    val stationuuid: String,
    val name: String,
    val url_resolved: String?,
    val url: String?,
    val favicon: String?,
    val tags: String?,
    val country: String?,
    val state: String?,
    val votes: Int?,
    val clickcount: Int?
) {
    fun toTrackEntity(): TrackEntity {
        val playableUrl = if (!url_resolved.isNullOrBlank()) {
            url_resolved
        } else {
            url ?: ""
        }
        
        // Clean up favicon if it's not a valid web URL or is missing
        val image = if (!favicon.isNullOrBlank() && (favicon.startsWith("http://") || favicon.startsWith("https://"))) {
            favicon
        } else {
            // A beautiful visual placeholder for Radio Streams
            "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=500&q=80"
        }
        
        val displayLocation = buildString {
            val s = state?.trim()
            val c = country?.trim()
            if (!s.isNullOrEmpty()) {
                append(s)
                if (!c.isNullOrEmpty()) append(", ")
            }
            if (!c.isNullOrEmpty()) append(c)
            if (isEmpty()) append("Live Radio Stream")
        }

        val displayTags = if (!tags.isNullOrBlank()) {
            tags.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(3)
                .joinToString(" • ")
        } else {
            "Live"
        }

        return TrackEntity(
            id = "radio_$stationuuid",
            title = name,
            artistName = displayLocation,
            albumName = displayTags,
            durationSeconds = 0, // 0 denotes real-time streaming
            streamUrl = playableUrl,
            coverUrl = image,
            isDownloaded = false
        )
    }
}

interface RadioApiService {
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("name") name: String? = null,
        @Query("country") country: String? = null,
        @Query("state") state: String? = null,
        @Query("tag") tag: String? = null,
        @Query("limit") limit: Int = 40,
        @Query("hidebroken") hidebroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true
    ): List<RadioStation>

    @GET("json/stations/topvote/40")
    suspend fun getTopStations(): List<RadioStation>
}

object RetrofitRadioClient {
    private const val BASE_URL = "https://de1.api.radio-browser.info/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val service: RadioApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(RadioApiService::class.java)
    }
}

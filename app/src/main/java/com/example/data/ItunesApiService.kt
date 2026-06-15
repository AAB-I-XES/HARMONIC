package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class ItunesTrack(
    val trackId: Long,
    val trackName: String?,
    val artistName: String?,
    val collectionName: String?,
    val trackTimeMillis: Int?,
    val artworkUrl100: String?,
    val previewUrl: String?
)

data class ItunesResponse(
    val resultCount: Int,
    val results: List<ItunesTrack>
)

interface ItunesApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("limit") limit: Int = 30,
        @Query("entity") entity: String = "song"
    ): ItunesResponse
}

object RetrofitMusicClient {
    private const val BASE_URL = "https://itunes.apple.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val service: ItunesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ItunesApiService::class.java)
    }

    // Helper to get high resolution artwork from low res urls returned by iTunes
    fun getHighResArtworkUrl(lowResUrl: String?): String {
        if (lowResUrl.isNullOrEmpty()) {
            return "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80"
        }
        return lowResUrl.replace("100x100bb.jpg", "500x500bb.jpg")
            .replace("100x100bf.jpg", "500x500bb.jpg")
            .replace("100x100bb.png", "500x500bb.png")
            .replace("60x60bb.jpg", "500x500bb.jpg")
            .replace("30x30bb.jpg", "500x500bb.jpg")
    }

    // High quality curated tracks for catalog and fallback.
    val curatedFallbackTracks = listOf(
        TrackEntity(
            id = "itunes_fallback_1",
            title = "Midnight Horizon",
            artistName = "Aether Flow",
            albumName = "Synthesized Echoes",
            durationSeconds = 240,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=500&q=80"
        ),
        TrackEntity(
            id = "itunes_fallback_2",
            title = "Acoustic Solitude",
            artistName = "David Rendon",
            albumName = "Golden Sessions",
            durationSeconds = 280,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"
        ),
        TrackEntity(
            id = "itunes_fallback_3",
            title = "Neon Luminescence",
            artistName = "Vector Wave",
            albumName = "Cyber Odyssey",
            durationSeconds = 300,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            coverUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=500&q=80"
        ),
        TrackEntity(
            id = "itunes_fallback_4",
            title = "Ethereal Dreamscape",
            artistName = "Lumina",
            albumName = "Cosmic Sanctuary",
            durationSeconds = 210,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80"
        ),
        TrackEntity(
            id = "itunes_fallback_5",
            title = "Velocity Chase",
            artistName = "Grid Runner",
            albumName = "Outrun Protocol",
            durationSeconds = 270,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=500&q=80"
        ),
        TrackEntity(
            id = "itunes_fallback_6",
            title = "Velvet Eclipse",
            artistName = "Seraphim",
            albumName = "Nocturnal Musings",
            durationSeconds = 290,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80"
        )
    )
}

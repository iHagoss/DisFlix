package com.stremio.app.api

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    
    const val STREMIO_API_URL = "https://api.strem.io/api/"
    const val CINEMETA_URL = "https://v3-cinemeta.strem.io/"
    const val CINEMETA_CATALOGS_URL = "https://cinemeta-catalogs.strem.io/"
    const val LINK_API_URL = "https://link.stremio.com/"
    const val USER_LIKES_URL = "https://likes.stremio.com/"
    const val STREAMING_SERVER_URL = "http://127.0.0.1:11470/"
    
    private val gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .setLenient()
        .registerTypeAdapterFactory(AuthPayloadTypeAdapterFactory())
        .registerTypeAdapterFactory(SaveUserPayloadTypeAdapterFactory())
        .create()
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    val stremioApi: StremioApi by lazy {
        createRetrofit(STREMIO_API_URL).create(StremioApi::class.java)
    }
    
    val cinemetaApi: CinemetaApi by lazy {
        createRetrofit(CINEMETA_URL).create(CinemetaApi::class.java)
    }
    
    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    fun createAddonApi(addonUrl: String): AddonApi {
        val url = if (addonUrl.endsWith("/")) addonUrl else "$addonUrl/"
        return createRetrofit(url).create(AddonApi::class.java)
    }
    
    fun createDynamicApi(baseUrl: String): AddonApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return createRetrofit(url).create(AddonApi::class.java)
    }
}

object OfficialAddons {
    val CINEMETA = AddonInfo(
        id = "com.linvo.cinemeta",
        name = "Cinemeta",
        url = "https://v3-cinemeta.strem.io/manifest.json",
        description = "The official addon for movie and series metadata",
        types = listOf("movie", "series"),
        isOfficial = true
    )
    
    val OPENSUBTITLES = AddonInfo(
        id = "org.stremio.opensubtitles",
        name = "OpenSubtitles",
        url = "https://opensubtitles-v3.strem.io/manifest.json",
        description = "Subtitles from OpenSubtitles",
        types = listOf("movie", "series"),
        isOfficial = true
    )
    
    val LOCAL_FILES = AddonInfo(
        id = "org.stremio.local",
        name = "Local Files",
        url = "http://127.0.0.1:11470/local-addon/manifest.json",
        description = "Play local files from your device",
        types = listOf("movie", "series", "other"),
        isOfficial = true
    )
    
    val YOUTUBE = AddonInfo(
        id = "com.stremio.youtube",
        name = "YouTube",
        url = "https://youtube-addon.strem.io/manifest.json",
        description = "YouTube videos and channels",
        types = listOf("channel"),
        isOfficial = true
    )
    
    val defaults = listOf(CINEMETA, OPENSUBTITLES, LOCAL_FILES, YOUTUBE)
}

data class AddonInfo(
    val id: String,
    val name: String,
    val url: String,
    val description: String,
    val types: List<String>,
    val isOfficial: Boolean
)

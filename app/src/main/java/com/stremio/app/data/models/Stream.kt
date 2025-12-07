package com.stremio.app.data.models

import com.google.gson.annotations.SerializedName

data class Stream(
    val url: String? = null,
    @SerializedName("ytId")
    val ytId: String? = null,
    @SerializedName("infoHash")
    val infoHash: String? = null,
    @SerializedName("fileIdx")
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val addonName: String? = null,
    val subtitles: List<Subtitles> = emptyList(),
    @SerializedName("behaviorHints")
    val behaviorHints: StreamBehaviorHints = StreamBehaviorHints()
) {
    val streamType: StreamType
        get() = when {
            url != null -> StreamType.URL
            ytId != null -> StreamType.YOUTUBE
            infoHash != null -> StreamType.TORRENT
            externalUrl != null -> StreamType.EXTERNAL
            else -> StreamType.UNKNOWN
        }
    
    val playableUrl: String?
        get() = when (streamType) {
            StreamType.URL -> url
            StreamType.YOUTUBE -> "https://www.youtube.com/watch?v=$ytId"
            StreamType.EXTERNAL -> externalUrl
            StreamType.TORRENT -> null
            StreamType.UNKNOWN -> null
        }
    
    val magnetUrl: String?
        get() = if (infoHash != null) {
            buildString {
                append("magnet:?xt=urn:btih:")
                append(infoHash)
                behaviorHints.bingeGroup?.let { append("&dn=$it") }
            }
        } else null
}

enum class StreamType {
    URL,
    YOUTUBE,
    TORRENT,
    EXTERNAL,
    UNKNOWN
}

data class Subtitles(
    val id: String,
    val url: String,
    val lang: String
)

data class StreamBehaviorHints(
    val notWebReady: Boolean = false,
    @SerializedName("bingeGroup")
    val bingeGroup: String? = null,
    @SerializedName("countryWhitelist")
    val countryWhitelist: List<String>? = null,
    @SerializedName("proxyHeaders")
    val proxyHeaders: StreamProxyHeaders? = null,
    val filename: String? = null,
    val videoSize: Long? = null,
    val videoHash: String? = null
)

data class StreamProxyHeaders(
    val request: Map<String, String> = emptyMap(),
    val response: Map<String, String> = emptyMap()
)

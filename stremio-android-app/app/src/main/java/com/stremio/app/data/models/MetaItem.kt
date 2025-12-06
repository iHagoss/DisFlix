package com.stremio.app.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class MetaItemPreview(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    @SerializedName("releaseInfo")
    val releaseInfo: String? = null,
    val runtime: String? = null,
    val released: Date? = null,
    @SerializedName("posterShape")
    val posterShape: PosterShape = PosterShape.POSTER,
    val links: List<Link> = emptyList(),
    @SerializedName("trailerStreams")
    val trailerStreams: List<Stream> = emptyList(),
    @SerializedName("behaviorHints")
    val behaviorHints: MetaItemBehaviorHints = MetaItemBehaviorHints()
)

data class MetaItem(
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    @SerializedName("releaseInfo")
    val releaseInfo: String? = null,
    val runtime: String? = null,
    val released: Date? = null,
    @SerializedName("posterShape")
    val posterShape: PosterShape = PosterShape.POSTER,
    val links: List<Link> = emptyList(),
    @SerializedName("trailerStreams")
    val trailerStreams: List<Stream> = emptyList(),
    @SerializedName("behaviorHints")
    val behaviorHints: MetaItemBehaviorHints = MetaItemBehaviorHints(),
    val videos: List<Video> = emptyList()
) {
    fun toPreview(): MetaItemPreview = MetaItemPreview(
        id = id,
        type = type,
        name = name,
        poster = poster,
        background = background,
        logo = logo,
        description = description,
        releaseInfo = releaseInfo,
        runtime = runtime,
        released = released,
        posterShape = posterShape,
        links = links,
        trailerStreams = trailerStreams,
        behaviorHints = behaviorHints
    )
    
    fun getVideosBySeason(season: Int): List<Video> {
        return videos.filter { it.season == season }
    }
    
    fun getSeasons(): List<Int> {
        return videos.mapNotNull { it.season }.distinct().sorted()
    }
}

enum class PosterShape {
    @SerializedName("square")
    SQUARE,
    @SerializedName("landscape")
    LANDSCAPE,
    @SerializedName("poster")
    POSTER
}

data class Video(
    val id: String,
    val title: String = "",
    val released: Date? = null,
    val overview: String? = null,
    val thumbnail: String? = null,
    val streams: List<Stream> = emptyList(),
    val season: Int? = null,
    val episode: Int? = null,
    @SerializedName("trailerStreams")
    val trailerStreams: List<Stream> = emptyList()
) {
    val seriesInfo: SeriesInfo?
        get() = if (season != null && episode != null) SeriesInfo(season, episode) else null
}

data class SeriesInfo(
    val season: Int,
    val episode: Int
)

data class Link(
    val name: String,
    val category: String,
    val url: String
)

data class MetaItemBehaviorHints(
    @SerializedName("defaultVideoId")
    val defaultVideoId: String? = null,
    @SerializedName("featuredVideoId")
    val featuredVideoId: String? = null,
    @SerializedName("hasScheduledVideos")
    val hasScheduledVideos: Boolean = false
)

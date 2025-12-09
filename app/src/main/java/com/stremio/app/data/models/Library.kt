package com.stremio.app.data.models

import com.google.gson.annotations.SerializedName
import java.util.Date

data class LibraryItem(
    @SerializedName("_id")
    val id: String,
    val type: String,
    val name: String,
    val poster: String? = null,
    @SerializedName("posterShape")
    val posterShape: PosterShape = PosterShape.POSTER,
    val removed: Boolean = false,
    val temp: Boolean = false,
    @SerializedName("_ctime")
    val createdTime: Date? = null,
    @SerializedName("_mtime")
    val modifiedTime: Date? = null,
    val state: LibraryItemState = LibraryItemState(),
    @SerializedName("behaviorHints")
    val behaviorHints: MetaItemBehaviorHints = MetaItemBehaviorHints()
) {
    val isWatched: Boolean
        get() {
            val duration = state.duration
            val timeWatched = state.timeWatched
            if (duration <= 0 || timeWatched <= 0) return false
            return timeWatched.toDouble() / duration.toDouble() > WATCHED_THRESHOLD
        }
    
    val progress: Float
        get() {
            val duration = state.duration
            val timeWatched = state.timeWatched
            if (duration <= 0) return 0f
            return (timeWatched.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
        }
    
    companion object {
        const val WATCHED_THRESHOLD = 0.7
    }
}

data class LibraryItemState(
    @SerializedName("lastWatched")
    val lastWatched: Date? = null,
    @SerializedName("timeWatched")
    val timeWatched: Long = 0,
    @SerializedName("timeOffset")
    val timeOffset: Long = 0,
    @SerializedName("overallTimeWatched")
    val overallTimeWatched: Long = 0,
    val duration: Long = 0,
    @SerializedName("video_id")
    val videoId: String? = null,
    @SerializedName("flaggedWatched")
    val flaggedWatched: Int = 0,
    @SerializedName("timesWatched")
    val timesWatched: Int = 0,
    val watched: WatchedBitfield? = null,
    @SerializedName("noNotif")
    val noNotification: Boolean = false
)

data class WatchedBitfield(
    val bitfield: String? = null
) {
    fun isVideoWatched(videoId: String): Boolean {
        return false
    }
}

data class LibrarySyncRequest(
    @SerializedName("authKey")
    val authKey: String,
    val collection: String = "libraryItem",
    val changes: List<LibraryItem> = emptyList()
)

data class LibraryBucket(
    val uid: String,
    val items: Map<String, LibraryItem> = emptyMap()
) {
    fun getRecentItems(limit: Int = 200): List<LibraryItem> {
        return items.values
            .filter { !it.removed && !it.temp }
            .sortedByDescending { it.state.lastWatched ?: it.modifiedTime }
            .take(limit)
    }
    
    fun getContinueWatching(): List<LibraryItem> {
        return items.values
            .filter { !it.removed && !it.temp && it.state.timeOffset > 0 && !it.isWatched }
            .sortedByDescending { it.state.lastWatched }
    }
    
    fun getItemsByType(type: String): List<LibraryItem> {
        return items.values
            .filter { !it.removed && !it.temp && it.type == type }
            .sortedByDescending { it.modifiedTime }
    }
}

data class ContinueWatchingItem(
    val libraryItem: LibraryItem,
    val metaItem: MetaItemPreview? = null,
    val video: Video? = null
) {
    val deepLink: String
        get() = "stremio:///detail/${libraryItem.type}/${libraryItem.id}"
}

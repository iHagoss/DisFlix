package com.stremio.app.data.models

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("_id")
    val id: String,
    val email: String,
    @SerializedName("fbId")
    val fbId: String? = null,
    val avatar: String? = null,
    @SerializedName("dateRegistered")
    val dateRegistered: String? = null,
    @SerializedName("lastModified")
    val lastModified: String? = null,
    @SerializedName("gdprConsent")
    val gdprConsent: GDPRConsent? = null,
    val trakt: TraktInfo? = null,
    val premium: PremiumInfo? = null
)

data class GDPRConsent(
    val marketing: Boolean = false,
    val privacy: Boolean = false,
    val tos: Boolean = false,
    val from: String? = null
)

data class TraktInfo(
    @SerializedName("access_token")
    val accessToken: String? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null
)

data class PremiumInfo(
    val active: Boolean = false,
    @SerializedName("expiresAt")
    val expiresAt: String? = null
)

data class Profile(
    val auth: AuthInfo? = null,
    val user: User? = null,
    val addons: List<Addon> = emptyList(),
    val settings: Settings = Settings()
) {
    val isLoggedIn: Boolean
        get() = auth != null && user != null
    
    val isPremium: Boolean
        get() = user?.premium?.active == true
}

data class AuthInfo(
    val key: String,
    @SerializedName("userId")
    val userId: String
)

data class Settings(
    @SerializedName("interfaceLanguage")
    val interfaceLanguage: String = "eng",
    @SerializedName("streamingServerUrl")
    val streamingServerUrl: String = "http://127.0.0.1:11470",
    @SerializedName("playerType")
    val playerType: String? = null,
    @SerializedName("bingeWatching")
    val bingeWatching: Boolean = true,
    @SerializedName("playInBackground")
    val playInBackground: Boolean = true,
    @SerializedName("hardwareDecoding")
    val hardwareDecoding: Boolean = true,
    @SerializedName("subtitlesLanguage")
    val subtitlesLanguage: String = "eng",
    @SerializedName("subtitlesSize")
    val subtitlesSize: Int = 100,
    @SerializedName("subtitlesFont")
    val subtitlesFont: String = "Roboto",
    @SerializedName("subtitlesBold")
    val subtitlesBold: Boolean = false,
    @SerializedName("subtitlesOffset")
    val subtitlesOffset: Int = 0,
    @SerializedName("subtitlesTextColor")
    val subtitlesTextColor: String = "#FFFFFFFF",
    @SerializedName("subtitlesBackgroundColor")
    val subtitlesBackgroundColor: String = "#00000000",
    @SerializedName("subtitlesOutlineColor")
    val subtitlesOutlineColor: String = "#FF000000",
    @SerializedName("autoFrameRateMatching")
    val autoFrameRateMatching: Boolean = false,
    @SerializedName("nextVideoNotificationDuration")
    val nextVideoNotificationDuration: Int = 35000,
    @SerializedName("audioPassthrough")
    val audioPassthrough: Boolean = false,
    @SerializedName("audioLanguage")
    val audioLanguage: String = "eng",
    @SerializedName("secondaryAudioLanguage")
    val secondaryAudioLanguage: String? = null,
    @SerializedName("secondarySubtitlesLanguage")
    val secondarySubtitlesLanguage: String? = null,
    @SerializedName("seekTimeDuration")
    val seekTimeDuration: Int = 10000,
    @SerializedName("seekShortTimeDuration")
    val seekShortTimeDuration: Int = 3000,
    @SerializedName("pauseOnMinimize")
    val pauseOnMinimize: Boolean = true,
    @SerializedName("surroundSound")
    val surroundSound: Boolean = false,
    @SerializedName("streamingServerWarningDismissed")
    val streamingServerWarningDismissed: String? = null
)

package com.stremio.app.api

import com.stremio.app.data.models.*
import retrofit2.Response
import retrofit2.http.*

interface StremioApi {
    
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("logout")
    suspend fun logout(@Body request: LogoutRequest): Response<Unit>
    
    @POST("getUser")
    suspend fun getUser(@Body request: GetUserRequest): Response<UserResponse>
    
    @POST("saveUser")
    suspend fun saveUser(@Body request: SaveUserRequest): Response<Unit>
    
    @POST("addonCollectionGet")
    suspend fun getAddonCollection(@Body request: AddonCollectionRequest): Response<AddonCollectionResponse>
    
    @POST("addonCollectionSet")
    suspend fun setAddonCollection(@Body request: SetAddonCollectionRequest): Response<Unit>
    
    @POST("datastoreMeta")
    suspend fun datastoreMeta(@Body request: DatastoreMetaRequest): Response<DatastoreMetaResponse>
    
    @POST("datastoreGet")
    suspend fun datastoreGet(@Body request: DatastoreGetRequest): Response<DatastoreGetResponse>
    
    @POST("datastorePut")
    suspend fun datastorePut(@Body request: DatastorePutRequest): Response<Unit>
    
    @POST("getSkipIntro")
    suspend fun getSkipIntro(@Body request: SkipIntroRequest): Response<SkipIntroResponse>
    
    @POST("getSkipGaps")
    suspend fun getSkipGaps(@Body request: SkipGapsRequest): Response<SkipGapsResponse>
    
    @POST("seekLog")
    suspend fun seekLog(@Body request: SeekLogRequest): Response<Unit>
    
    @POST("events")
    suspend fun sendEvents(@Body request: EventsRequest): Response<Unit>
    
    @POST("getModal")
    suspend fun getModal(@Body request: GetModalRequest): Response<ModalResponse>
    
    @POST("getNotification")
    suspend fun getNotification(@Body request: GetNotificationRequest): Response<NotificationResponse>
}

data class LoginRequest(
    val type: String = "Login",
    val email: String,
    val password: String,
    val facebook: Boolean = false
)

data class RegisterRequest(
    val type: String = "Register",
    val email: String,
    val password: String,
    val gdprConsent: GDPRConsent
)

data class LogoutRequest(
    val type: String = "Logout",
    val authKey: String
)

data class GetUserRequest(
    val type: String = "GetUser",
    val authKey: String
)

data class SaveUserRequest(
    val type: String = "SaveUser",
    val authKey: String,
    val user: User
)

data class AddonCollectionRequest(
    val type: String = "AddonCollectionGet",
    val authKey: String,
    val update: Boolean = false
)

data class SetAddonCollectionRequest(
    val type: String = "AddonCollectionSet",
    val authKey: String,
    val addons: List<Addon>
)

data class DatastoreMetaRequest(
    val authKey: String,
    val collection: String = "libraryItem"
)

data class DatastoreGetRequest(
    val authKey: String,
    val collection: String = "libraryItem",
    val ids: List<String> = emptyList(),
    val all: Boolean = false
)

data class DatastorePutRequest(
    val authKey: String,
    val collection: String = "libraryItem",
    val changes: List<LibraryItem>
)

data class SkipIntroRequest(
    val authKey: String,
    val osId: String,
    val itemId: String,
    val season: Int? = null,
    val episode: Int? = null,
    val stHash: String
)

data class SkipGapsRequest(
    val authKey: String,
    val osId: String,
    val itemId: String,
    val season: Int? = null,
    val episode: Int? = null,
    val stHash: String
)

data class SeekLogRequest(
    val osId: String,
    val itemId: String,
    val season: Int? = null,
    val episode: Int? = null,
    val stHash: String,
    val duration: Long,
    val seekHistory: List<SeekLog>,
    val skipOutro: List<Long> = emptyList()
)

data class SeekLog(
    val from: Long,
    val to: Long
)

data class EventsRequest(
    val authKey: String,
    val events: List<Map<String, Any>>
)

data class GetModalRequest(
    val date: String
)

data class GetNotificationRequest(
    val date: String
)

data class AuthResponse(
    val result: AuthResult? = null,
    val error: ApiError? = null
)

data class AuthResult(
    val authKey: String,
    val user: User
)

data class UserResponse(
    val result: User? = null,
    val error: ApiError? = null
)

data class AddonCollectionResponse(
    val result: AddonCollectionResult? = null,
    val error: ApiError? = null
)

data class AddonCollectionResult(
    val addons: List<Addon>,
    val lastModified: String?
)

data class DatastoreMetaResponse(
    val result: List<DatastoreMeta>? = null,
    val error: ApiError? = null
)

data class DatastoreMeta(
    val id: String,
    val mtime: String
)

data class DatastoreGetResponse(
    val result: List<LibraryItem>? = null,
    val error: ApiError? = null
)

data class SkipIntroResponse(
    val result: SkipIntroData? = null,
    val error: ApiError? = null
)

data class SkipIntroData(
    val intro: SkipSegment? = null,
    val credits: SkipSegment? = null
)

data class SkipSegment(
    val start: Long,
    val end: Long
)

data class SkipGapsResponse(
    val result: SkipGapsData? = null,
    val error: ApiError? = null
)

data class SkipGapsData(
    val gaps: List<SkipGap> = emptyList()
)

data class SkipGap(
    val start: Long,
    val end: Long,
    val type: String
)

data class ModalResponse(
    val result: ModalData? = null
)

data class ModalData(
    val type: String,
    val title: String?,
    val message: String?,
    val buttons: List<ModalButton> = emptyList()
)

data class ModalButton(
    val label: String,
    val action: String
)

data class NotificationResponse(
    val result: NotificationData? = null
)

data class NotificationData(
    val id: String,
    val title: String,
    val message: String,
    val items: List<MetaItemPreview> = emptyList()
)

data class ApiError(
    val code: Int,
    val message: String
)

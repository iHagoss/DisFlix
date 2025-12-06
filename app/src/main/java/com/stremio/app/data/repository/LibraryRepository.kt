package com.stremio.app.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stremio.app.api.ApiClient
import com.stremio.app.api.DatastoreGetRequest
import com.stremio.app.api.DatastoreMetaRequest
import com.stremio.app.api.DatastorePutRequest
import com.stremio.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class LibraryRepository(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    
    companion object {
        private const val TAG = "LibraryRepository"
        private const val PREFS_NAME = "stremio_library"
        private const val KEY_LIBRARY = "library_items"
        private const val KEY_LAST_SYNC = "last_sync"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val libraryItems = mutableMapOf<String, LibraryItem>()
    private var lastSyncTime: Long = 0
    
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            loadLocalLibrary()
            if (authRepository.isLoggedIn) {
                syncLibrary()
            }
        }
    }
    
    private fun loadLocalLibrary() {
        val libraryJson = prefs.getString(KEY_LIBRARY, null) ?: return
        try {
            val type = object : TypeToken<Map<String, LibraryItem>>() {}.type
            val items: Map<String, LibraryItem> = gson.fromJson(libraryJson, type)
            libraryItems.clear()
            libraryItems.putAll(items)
            lastSyncTime = prefs.getLong(KEY_LAST_SYNC, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local library", e)
        }
    }
    
    private fun saveLocalLibrary() {
        prefs.edit()
            .putString(KEY_LIBRARY, gson.toJson(libraryItems))
            .putLong(KEY_LAST_SYNC, lastSyncTime)
            .apply()
    }
    
    suspend fun syncLibrary(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val authKey = authRepository.authKey ?: return@withContext Result.failure(Exception("Not logged in"))
                
                val response = ApiClient.stremioApi.datastoreGet(
                    DatastoreGetRequest(authKey = authKey, all = true)
                )
                
                if (response.isSuccessful && response.body()?.result != null) {
                    val items = response.body()!!.result!!
                    libraryItems.clear()
                    items.forEach { item ->
                        libraryItems[item.id] = item
                    }
                    lastSyncTime = System.currentTimeMillis()
                    saveLocalLibrary()
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Failed to sync library"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync library error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun addToLibrary(metaItem: MetaItemPreview): Result<LibraryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val now = Date()
                val libraryItem = LibraryItem(
                    id = metaItem.id,
                    type = metaItem.type,
                    name = metaItem.name,
                    poster = metaItem.poster,
                    posterShape = metaItem.posterShape,
                    createdTime = now,
                    modifiedTime = now,
                    behaviorHints = metaItem.behaviorHints
                )
                
                libraryItems[libraryItem.id] = libraryItem
                saveLocalLibrary()
                
                if (authRepository.isLoggedIn) {
                    pushToServer(listOf(libraryItem))
                }
                
                Result.success(libraryItem)
            } catch (e: Exception) {
                Log.e(TAG, "Add to library error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun removeFromLibrary(itemId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val item = libraryItems[itemId] ?: return@withContext Result.failure(Exception("Item not found"))
                
                val removedItem = item.copy(removed = true, modifiedTime = Date())
                libraryItems[itemId] = removedItem
                saveLocalLibrary()
                
                if (authRepository.isLoggedIn) {
                    pushToServer(listOf(removedItem))
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Remove from library error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun updateProgress(
        itemId: String,
        videoId: String,
        timeOffset: Long,
        duration: Long
    ): Result<LibraryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val item = libraryItems[itemId]
                val now = Date()
                
                val updatedItem = (item ?: createNewLibraryItem(itemId)).copy(
                    modifiedTime = now,
                    state = (item?.state ?: LibraryItemState()).copy(
                        lastWatched = now,
                        timeOffset = timeOffset,
                        duration = duration,
                        video = videoId,
                        timeWatched = (item?.state?.timeWatched ?: 0) + timeOffset,
                        overallTimeWatched = (item?.state?.overallTimeWatched ?: 0) + timeOffset
                    )
                )
                
                libraryItems[itemId] = updatedItem
                saveLocalLibrary()
                
                if (authRepository.isLoggedIn) {
                    pushToServer(listOf(updatedItem))
                }
                
                Result.success(updatedItem)
            } catch (e: Exception) {
                Log.e(TAG, "Update progress error", e)
                Result.failure(e)
            }
        }
    }
    
    private fun createNewLibraryItem(itemId: String): LibraryItem {
        val now = Date()
        return LibraryItem(
            id = itemId,
            type = "movie",
            name = "",
            createdTime = now,
            modifiedTime = now
        )
    }
    
    private suspend fun pushToServer(items: List<LibraryItem>) {
        try {
            val authKey = authRepository.authKey ?: return
            
            ApiClient.stremioApi.datastorePut(
                DatastorePutRequest(authKey = authKey, changes = items)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Push to server error", e)
        }
    }
    
    fun getLibraryItem(itemId: String): LibraryItem? = libraryItems[itemId]
    
    fun getAllItems(): List<LibraryItem> {
        return libraryItems.values
            .filter { !it.removed && !it.temp }
            .sortedByDescending { it.modifiedTime }
    }
    
    fun getRecentItems(limit: Int = 200): List<LibraryItem> {
        return libraryItems.values
            .filter { !it.removed && !it.temp }
            .sortedByDescending { it.state.lastWatched ?: it.modifiedTime }
            .take(limit)
    }
    
    fun getContinueWatching(): List<LibraryItem> {
        return libraryItems.values
            .filter { !it.removed && !it.temp && it.state.timeOffset > 0 && !it.isWatched }
            .sortedByDescending { it.state.lastWatched }
    }
    
    fun getItemsByType(type: String): List<LibraryItem> {
        return libraryItems.values
            .filter { !it.removed && !it.temp && it.type == type }
            .sortedByDescending { it.modifiedTime }
    }
    
    fun isInLibrary(itemId: String): Boolean {
        val item = libraryItems[itemId]
        return item != null && !item.removed
    }
}

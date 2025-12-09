package com.stremio.app.addon

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stremio.app.api.ApiClient
import com.stremio.app.api.AddonCollectionRequest
import com.stremio.app.api.OfficialAddons
import com.stremio.app.data.models.*
import com.stremio.app.data.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class AddonManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AddonManager"
        private const val PREFS_NAME = "stremio_addons"
        private const val KEY_INSTALLED_ADDONS = "installed_addons"
        private const val KEY_ADDON_MANIFESTS = "addon_manifests"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val installedAddons = mutableListOf<Addon>()
    private val addonCache = mutableMapOf<String, Addon>()
    
    private var authRepository: AuthRepository? = null
    
    fun setAuthRepository(authRepository: AuthRepository) {
        this.authRepository = authRepository
    }
    
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            loadCachedAddons()
            
            val auth = authRepository
            if (auth != null && auth.isLoggedIn) {
                try {
                    syncAddonsFromServer()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync addons from server", e)
                    if (installedAddons.isEmpty()) {
                        installDefaultAddons()
                    }
                }
            } else {
                if (installedAddons.isEmpty()) {
                    installDefaultAddons()
                }
            }
            
            Log.d(TAG, "AddonManager initialized with ${installedAddons.size} addons")
        }
    }
    
    private fun loadCachedAddons() {
        try {
            val manifestsJson = prefs.getString(KEY_ADDON_MANIFESTS, null)
            if (manifestsJson != null) {
                val type = object : TypeToken<List<Addon>>() {}.type
                val addons: List<Addon> = gson.fromJson(manifestsJson, type)
                installedAddons.clear()
                installedAddons.addAll(addons)
                addons.forEach { addonCache[it.manifest.id] = it }
                Log.d(TAG, "Loaded ${addons.size} cached addons")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached addons", e)
        }
    }
    
    private fun saveAddonsToCache() {
        try {
            val manifestsJson = gson.toJson(installedAddons)
            prefs.edit().putString(KEY_ADDON_MANIFESTS, manifestsJson).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save addons to cache", e)
        }
    }
    
    suspend fun syncAddonsFromServer(): Result<List<Addon>> {
        return withContext(Dispatchers.IO) {
            try {
                val authKey = authRepository?.authKey
                    ?: return@withContext Result.failure(Exception("Not logged in"))
                
                Log.d(TAG, "Syncing addons from server...")
                
                val response = ApiClient.stremioApi.getAddonCollection(
                    AddonCollectionRequest(authKey = authKey, update = true)
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.error != null) {
                        Log.e(TAG, "API error: ${body.error.message}")
                        return@withContext Result.failure(Exception(body.error.message))
                    }
                    
                    val serverAddons = body?.result?.addons ?: emptyList()
                    Log.d(TAG, "Received ${serverAddons.size} addons from server")
                    
                    if (serverAddons.isNotEmpty()) {
                        installedAddons.clear()
                        addonCache.clear()
                        
                        for (addon in serverAddons) {
                            installedAddons.add(addon)
                            addonCache[addon.manifest.id] = addon
                        }
                        
                        saveAddonsToCache()
                    } else {
                        installDefaultAddons()
                    }
                    
                    Result.success(installedAddons.toList())
                } else {
                    Log.e(TAG, "Sync failed: ${response.code()}")
                    Result.failure(Exception("Failed to sync addons: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync addons error", e)
                Result.failure(e)
            }
        }
    }
    
    private suspend fun installDefaultAddons() {
        Log.d(TAG, "Installing default addons...")
        for (addonInfo in OfficialAddons.defaults) {
            try {
                val addon = fetchAddon(addonInfo.url)
                if (addon != null && installedAddons.none { it.manifest.id == addon.manifest.id }) {
                    installedAddons.add(addon)
                    addonCache[addon.manifest.id] = addon
                    Log.d(TAG, "Installed default addon: ${addon.manifest.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install default addon: ${addonInfo.name}", e)
            }
        }
        saveAddonsToCache()
    }
    
    suspend fun fetchAddon(manifestUrl: String): Addon? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = if (manifestUrl.endsWith("/manifest.json")) {
                    manifestUrl.removeSuffix("/manifest.json")
                } else {
                    manifestUrl.removeSuffix("/")
                }
                
                val api = ApiClient.createAddonApi(baseUrl)
                val response = api.getManifest()
                
                if (response.isSuccessful && response.body() != null) {
                    val manifest = response.body()!!
                    val addon = Addon(
                        manifest = manifest,
                        transportUrl = baseUrl,
                        flags = AddonFlags(
                            official = OfficialAddons.defaults.any { it.id == manifest.id }
                        )
                    )
                    Log.d(TAG, "Fetched addon: ${manifest.name} (${manifest.id})")
                    addon
                } else {
                    Log.e(TAG, "Failed to fetch addon from $manifestUrl: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching addon: $manifestUrl", e)
                null
            }
        }
    }
    
    suspend fun installAddon(manifestUrl: String): Boolean {
        val addon = fetchAddon(manifestUrl) ?: return false
        
        if (installedAddons.none { it.manifest.id == addon.manifest.id }) {
            installedAddons.add(addon)
            addonCache[addon.manifest.id] = addon
            saveAddonsToCache()
            
            authRepository?.authKey?.let { authKey ->
                try {
                    ApiClient.stremioApi.setAddonCollection(
                        com.stremio.app.api.SetAddonCollectionRequest(
                            authKey = authKey,
                            addons = installedAddons
                        )
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync addon to server", e)
                }
            }
            
            return true
        }
        return false
    }
    
    fun uninstallAddon(addonId: String): Boolean {
        val removed = installedAddons.removeAll { it.manifest.id == addonId }
        if (removed) {
            addonCache.remove(addonId)
            saveAddonsToCache()
        }
        return removed
    }
    
    fun getInstalledAddons(): List<Addon> = installedAddons.toList()
    
    fun getAddon(addonId: String): Addon? = addonCache[addonId]
    
    fun getAddonsForResource(resource: String, type: String): List<Addon> {
        return installedAddons.filter { addon ->
            addon.manifest.resources.any { res ->
                res.name == resource && (res.types?.contains(type) ?: addon.manifest.types.contains(type))
            }
        }
    }
    
    fun getAddonsWithCatalog(type: String): List<Addon> {
        return installedAddons.filter { addon ->
            addon.manifest.catalogs.any { it.type == type }
        }
    }
    
    fun getAllCatalogs(): List<Pair<Addon, ManifestCatalog>> {
        return installedAddons.flatMap { addon ->
            addon.manifest.catalogs.map { catalog -> addon to catalog }
        }
    }
    
    fun getCatalogsForType(type: String): List<Pair<Addon, ManifestCatalog>> {
        return getAllCatalogs().filter { (_, catalog) -> catalog.type == type }
    }
    
    suspend fun getCatalog(
        addon: Addon,
        catalog: ManifestCatalog,
        extra: List<ExtraValue> = emptyList(),
        skip: Int = 0
    ): CatalogResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val api = ApiClient.createAddonApi(addon.transportUrl)
                
                val response = when {
                    skip > 0 -> api.getCatalogWithSkip(catalog.type, catalog.id, skip)
                    extra.isNotEmpty() -> {
                        val extraStr = extra.joinToString("&") { "${it.name}=${it.value}" }
                        api.getCatalogWithExtra(catalog.type, catalog.id, extraStr)
                    }
                    else -> api.getCatalog(catalog.type, catalog.id)
                }
                
                if (response.isSuccessful) {
                    val result = response.body()
                    Log.d(TAG, "Catalog ${catalog.id}: ${result?.metas?.size ?: 0} items")
                    result
                } else {
                    Log.e(TAG, "Failed to get catalog ${catalog.id}: ${response.code()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching catalog", e)
                null
            }
        }
    }
    
    suspend fun getMeta(addon: Addon, type: String, id: String): MetaItem? {
        return withContext(Dispatchers.IO) {
            try {
                val api = ApiClient.createAddonApi(addon.transportUrl)
                val response = api.getMeta(type, id)
                if (response.isSuccessful) response.body()?.meta else null
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching meta", e)
                null
            }
        }
    }
    
    suspend fun getStreams(type: String, id: String, videoId: String? = null): List<Pair<Addon, Stream>> {
        return coroutineScope {
            val streamAddons = getAddonsForResource("stream", type)
            
            streamAddons.map { addon ->
                async {
                    try {
                        val api = ApiClient.createAddonApi(addon.transportUrl)
                        val response = if (videoId != null) {
                            api.getStreamsForVideo(type, id, videoId)
                        } else {
                            api.getStreams(type, id)
                        }
                        
                        if (response.isSuccessful) {
                            response.body()?.streams?.map { addon to it } ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching streams from ${addon.manifest.name}", e)
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }
    
    suspend fun getSubtitles(type: String, id: String, extra: Map<String, String> = emptyMap()): List<Subtitles> {
        return coroutineScope {
            val subtitleAddons = getAddonsForResource("subtitles", type)
            
            subtitleAddons.map { addon ->
                async {
                    try {
                        val api = ApiClient.createAddonApi(addon.transportUrl)
                        val response = if (extra.isNotEmpty()) {
                            val extraStr = extra.entries.joinToString("&") { "${it.key}=${it.value}" }
                            api.getSubtitlesWithExtra(type, id, extraStr)
                        } else {
                            api.getSubtitles(type, id)
                        }
                        
                        if (response.isSuccessful) {
                            response.body()?.subtitles ?: emptyList()
                        } else {
                            emptyList()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching subtitles from ${addon.manifest.name}", e)
                        emptyList()
                    }
                }
            }.awaitAll().flatten()
        }
    }
    
    suspend fun getMetaItem(type: String, id: String): MetaItem? {
        val addons = getAddonsForResource("meta", type)
        for (addon in addons) {
            try {
                val meta = getMeta(addon, type, id)
                if (meta != null) return meta
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching meta from ${addon.manifest.name}", e)
            }
        }
        return null
    }
    
    suspend fun search(query: String, types: List<String> = listOf("movie", "series")): List<SearchResult> {
        return coroutineScope {
            val results = mutableListOf<SearchResult>()
            
            for (type in types) {
                val catalogAddons = getAddonsWithCatalog(type)
                
                for (addon in catalogAddons) {
                    val searchCatalogs = addon.manifest.catalogs.filter { catalog ->
                        catalog.type == type && catalog.isExtraSupported("search")
                    }
                    
                    for (catalog in searchCatalogs) {
                        try {
                            val catalogResult = getCatalog(
                                addon, catalog,
                                extra = listOf(ExtraValue("search", query))
                            )
                            
                            if (catalogResult != null && catalogResult.metas.isNotEmpty()) {
                                results.add(SearchResult(query, catalogResult.metas, addon))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Search error in ${addon.manifest.name}", e)
                        }
                    }
                }
            }
            
            results
        }
    }
}

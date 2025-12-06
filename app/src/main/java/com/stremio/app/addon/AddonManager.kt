package com.stremio.app.addon

import android.content.Context
import android.util.Log
import com.stremio.app.api.ApiClient
import com.stremio.app.api.OfficialAddons
import com.stremio.app.data.models.*
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
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val installedAddons = mutableListOf<Addon>()
    private val addonCache = mutableMapOf<String, Addon>()
    
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            loadInstalledAddons()
            if (installedAddons.isEmpty()) {
                installDefaultAddons()
            }
        }
    }
    
    private suspend fun loadInstalledAddons() {
        val savedAddons = prefs.getStringSet(KEY_INSTALLED_ADDONS, emptySet()) ?: emptySet()
        for (url in savedAddons) {
            try {
                val addon = fetchAddon(url)
                if (addon != null) {
                    installedAddons.add(addon)
                    addonCache[addon.manifest.id] = addon
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load addon: $url", e)
            }
        }
    }
    
    private suspend fun installDefaultAddons() {
        for (addonInfo in OfficialAddons.defaults) {
            try {
                val addon = fetchAddon(addonInfo.url)
                if (addon != null) {
                    installedAddons.add(addon)
                    addonCache[addon.manifest.id] = addon
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install default addon: ${addonInfo.name}", e)
            }
        }
        saveInstalledAddons()
    }
    
    suspend fun fetchAddon(manifestUrl: String): Addon? {
        return withContext(Dispatchers.IO) {
            try {
                val baseUrl = manifestUrl.removeSuffix("/manifest.json")
                val api = ApiClient.createAddonApi(baseUrl)
                val response = api.getManifest()
                
                if (response.isSuccessful && response.body() != null) {
                    val manifest = response.body()!!
                    Addon(
                        manifest = manifest,
                        transportUrl = baseUrl,
                        flags = AddonFlags(
                            official = OfficialAddons.defaults.any { it.id == manifest.id }
                        )
                    )
                } else {
                    Log.e(TAG, "Failed to fetch addon: ${response.code()}")
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
            saveInstalledAddons()
            return true
        }
        return false
    }
    
    fun uninstallAddon(addonId: String): Boolean {
        val removed = installedAddons.removeAll { it.manifest.id == addonId }
        if (removed) {
            addonCache.remove(addonId)
            saveInstalledAddons()
        }
        return removed
    }
    
    private fun saveInstalledAddons() {
        val urls = installedAddons.map { "${it.transportUrl}/manifest.json" }.toSet()
        prefs.edit().putStringSet(KEY_INSTALLED_ADDONS, urls).apply()
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
                
                if (response.isSuccessful) response.body() else null
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

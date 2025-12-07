package com.stremio.app

import android.content.Context
import android.util.Log

object StremioCore {
    private const val TAG = "StremioCore"
    private var isLoaded = false
    
    init {
        try {
            System.loadLibrary("stremio_core")
            isLoaded = true
            Log.i(TAG, "Native library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "Native library not available - using API fallback: ${e.message}")
            isLoaded = false
        } catch (e: Exception) {
            Log.w(TAG, "Native library initialization failed: ${e.message}")
            isLoaded = false
        }
    }
    
    fun isLibraryLoaded(): Boolean = isLoaded

    private external fun nativeInitCore(context: Context)
    private external fun nativeGetAddons(): String
    private external fun nativeGetLibrary(): String
    private external fun nativeSearch(query: String): String
    private external fun nativeGetAddonCatalog(addonId: String): String
    private external fun nativeInvokeAddon(addonId: String, method: String, args: String): String
    private external fun nativeDispatchAction(action: String, payload: String): String
    private external fun nativeGetSkipIntroData(itemId: String, duration: Long): String
    private external fun nativeShutdown()
    
    fun initCore(context: Context) {
        if (isLoaded) {
            try {
                nativeInitCore(context)
            } catch (e: Exception) {
                Log.e(TAG, "initCore failed: ${e.message}")
            }
        }
    }
    
    fun getAddons(): String {
        return if (isLoaded) {
            try { nativeGetAddons() } catch (e: Exception) { "[]" }
        } else "[]"
    }
    
    fun getLibrary(): String {
        return if (isLoaded) {
            try { nativeGetLibrary() } catch (e: Exception) { "[]" }
        } else "[]"
    }
    
    fun search(query: String): String {
        return if (isLoaded) {
            try { nativeSearch(query) } catch (e: Exception) { "[]" }
        } else "[]"
    }
    
    fun getAddonCatalog(addonId: String): String {
        return if (isLoaded) {
            try { nativeGetAddonCatalog(addonId) } catch (e: Exception) { "{}" }
        } else "{}"
    }
    
    fun invokeAddon(addonId: String, method: String, args: String): String {
        return if (isLoaded) {
            try { nativeInvokeAddon(addonId, method, args) } catch (e: Exception) { "{}" }
        } else "{}"
    }
    
    fun dispatchAction(action: String, payload: String): String {
        return if (isLoaded) {
            try { nativeDispatchAction(action, payload) } catch (e: Exception) {
                "{\"success\":false,\"error\":\"${e.message}\"}"
            }
        } else "{\"success\":false,\"error\":\"Library not loaded\"}"
    }
    
    fun getSkipIntroData(itemId: String, duration: Long): String {
        return if (isLoaded) {
            try { nativeGetSkipIntroData(itemId, duration) } catch (e: Exception) { "{}" }
        } else "{}"
    }
    
    fun shutdown() {
        if (isLoaded) {
            try { nativeShutdown() } catch (e: Exception) {
                Log.e(TAG, "shutdown failed: ${e.message}")
            }
        }
    }
    
    fun dispatchSkipIntro(): String {
        return dispatchAction("Player.SkipIntro", "{}")
    }
    
    fun dispatchSeek(time: Long, duration: Long): String {
        return dispatchAction("Player.Seek", "{\"time\":$time,\"duration\":$duration}")
    }
}

package com.stremio.app

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.json.JSONArray

object StremioCore {
    private const val TAG = "StremioCore"
    private var isLoaded = false

    init {
        try {
            System.loadLibrary("stremio_core_android")
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

    fun initCore(context: Context): Boolean = initialize(context)

    fun dispatchSkipIntro() {
        dispatchAction("skipIntro", "{}")
    }

    fun isLibraryLoaded(): Boolean = isLoaded

    // Native method declarations (will be implemented via JNI when stremio-core-kotlin is built)
    private external fun nativeInitialize(context: String): Boolean
    private external fun nativeGetAddons(): String
    private external fun nativeGetLibrary(): String
    private external fun nativeSearch(query: String): String
    private external fun nativeDispatchAction(action: String, payload: String): String
    private external fun nativeGetSkipIntroData(itemId: String, duration: Long): String
    private external fun nativeInvokeAddon(addonId: String, method: String, args: String): String
    private external fun nativeShutdown()
    // Streaming Server integration
    private external fun nativeGetStreamingServerSettings(): String
    private external fun nativeUpdateStreamingServerSettings(settingsJson: String): String
    private external fun nativeTestStreamingServerConnection(url: String): String
    private external fun nativeGetStreamingServerStats(): String


    /**
     * Initialize the Stremio Core runtime
     */
    fun initialize(context: Context): Boolean {
        return if (isLoaded) {
            try {
                val contextJson = JSONObject().apply {
                    put("appVersion", "1.0.0")
                    put("platform", "android")
                }.toString()
                nativeInitialize(contextJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize native core", e)
                false
            }
        } else {
            Log.w(TAG, "Core initialized in fallback mode (no native library)")
            true // Return true for fallback mode
        }
    }

    /**
     * Get installed addons
     */
    fun getAddons(): String {
        return if (isLoaded) {
            try {
                nativeGetAddons()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get addons from native", e)
                getFallbackAddons()
            }
        } else {
            getFallbackAddons()
        }
    }

    /**
     * Get user library
     */
    fun getLibrary(): String {
        return if (isLoaded) {
            try {
                nativeGetLibrary()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get library from native", e)
                getFallbackLibrary()
            }
        } else {
            getFallbackLibrary()
        }
    }

    /**
     * Search for content
     */
    fun search(query: String): String {
        return if (isLoaded) {
            try {
                nativeSearch(query)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to search from native", e)
                getFallbackSearch(query)
            }
        } else {
            getFallbackSearch(query)
        }
    }

    /**
     * Dispatch an action to the core
     */
    fun dispatchAction(action: String, payload: String): String {
        return if (isLoaded) {
            try {
                nativeDispatchAction(action, payload)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dispatch action from native", e)
                getFallbackResponse(action)
            }
        } else {
            getFallbackResponse(action)
        }
    }

    /**
     * Get skip intro data for a video
     */
    fun getSkipIntroData(itemId: String, duration: Long): String {
        return if (isLoaded) {
            try {
                nativeGetSkipIntroData(itemId, duration)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get skip intro data from native", e)
                getFallbackSkipIntro()
            }
        } else {
            getFallbackSkipIntro()
        }
    }

    /**
     * Invoke an addon method
     */
    fun invokeAddon(addonId: String, method: String, args: String): String {
        return if (isLoaded) {
            try {
                nativeInvokeAddon(addonId, method, args)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to invoke addon from native", e)
                getFallbackAddonResponse(addonId, method)
            }
        } else {
            getFallbackAddonResponse(addonId, method)
        }
    }

    /**
     * Shutdown the core
     */
    fun shutdown() {
        if (isLoaded) {
            try {
                nativeShutdown()
                Log.i(TAG, "Native core shutdown complete")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to shutdown native core", e)
            }
        }
    }

    /**
     * Get streaming server settings
     */
    fun getStreamingServerSettings(): String {
        return if (isLoaded) {
            try {
                nativeGetStreamingServerSettings()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get streaming server settings from native", e)
                getFallbackStreamingServerSettings()
            }
        } else {
            getFallbackStreamingServerSettings()
        }
    }

    /**
     * Update streaming server settings
     */
    fun updateStreamingServerSettings(settingsJson: String): String {
        return if (isLoaded) {
            try {
                nativeUpdateStreamingServerSettings(settingsJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update streaming server settings from native", e)
                getFallbackUpdateStreamingServerSettings()
            }
        } else {
            getFallbackUpdateStreamingServerSettings()
        }
    }

    /**
     * Test streaming server connection
     */
    fun testStreamingServerConnection(url: String): String {
        return if (isLoaded) {
            try {
                nativeTestStreamingServerConnection(url)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to test streaming server connection from native", e)
                getFallbackTestStreamingServerConnection()
            }
        } else {
            getFallbackTestStreamingServerConnection()
        }
    }

    /**
     * Get streaming server statistics
     */
    fun getStreamingServerStats(): String {
        return if (isLoaded) {
            try {
                nativeGetStreamingServerStats()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get streaming server stats from native", e)
                getFallbackStreamingServerStats()
            }
        } else {
            getFallbackStreamingServerStats()
        }
    }

    // Fallback implementations that return valid JSON structures
    // These will be used when the native library is not available

    private fun getFallbackAddons(): String {
        return JSONObject().apply {
            put("addons", JSONArray())
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackLibrary(): String {
        return JSONObject().apply {
            put("items", JSONArray())
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackSearch(query: String): String {
        return JSONObject().apply {
            put("query", query)
            put("results", JSONArray())
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackResponse(action: String): String {
        return JSONObject().apply {
            put("action", action)
            put("success", false)
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackSkipIntro(): String {
        return JSONObject().apply {
            put("hasIntro", false)
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackAddonResponse(addonId: String, method: String): String {
        return JSONObject().apply {
            put("addonId", addonId)
            put("method", method)
            put("result", JSONObject())
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackStreamingServerSettings(): String {
        return JSONObject().apply {
            put("settings", JSONObject())
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackUpdateStreamingServerSettings(): String {
        return JSONObject().apply {
            put("success", false)
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackTestStreamingServerConnection(): String {
        return JSONObject().apply {
            put("success", false)
            put("status", "fallback")
        }.toString()
    }

    private fun getFallbackStreamingServerStats(): String {
        return JSONObject().apply {
            put("stats", JSONObject())
            put("status", "fallback")
        }.toString()
    }
}
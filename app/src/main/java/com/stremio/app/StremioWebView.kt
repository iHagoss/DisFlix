package com.stremio.app

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings

class StremioWebView(context: Context) : WebView(context) {
    companion object {
        private const val TAG = "StremioWebView"
        private const val WEB_ASSET_PATH = "web/index.html"
    }

    init {
        configureWebView()
        // The API bridge must be added BEFORE the web page loads.
        addJavascriptInterface(StremioCoreBridge(), "StremioCore")
        loadStremioWeb()
    }

    // Inner class to bridge JavaScript calls to Kotlin/JNI functions in StremioCore
    private inner class StremioCoreBridge {
        
        // This annotation is crucial for exposing methods to JavaScript
        @JavascriptInterface
        fun getAddons(): String {
            return StremioCore.getAddons()
        }

        @JavascriptInterface
        fun getLibrary(): String {
            return StremioCore.getLibrary()
        }

        @JavascriptInterface
        fun search(query: String): String {
            return StremioCore.search(query)
        }

        @JavascriptInterface
        fun dispatchAction(action: String, payload: String): String {
            return StremioCore.dispatchAction(action, payload)
        }

        @JavascriptInterface
        fun getSkipIntroData(itemId: String, duration: Long): String {
            return StremioCore.getSkipIntroData(itemId, duration)
        }
        
        @JavascriptInterface
        fun invokeAddon(addonId: String, method: String, args: String): String {
            return StremioCore.invokeAddon(addonId, method, args)
        }
    }

    private fun configureWebView() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            // Setting this is vital for web apps loaded via file:///
            allowUniversalAccessFromFileURLs = true 
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Stremio/Android"
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished: $url")
                injectStremioAPI()
            }

            override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "Web error: ${request?.url} - ${error?.description}")
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                // Allows us to see JS logs in Logcat
                Log.d(TAG, "[JS Console] ${consoleMessage?.message()}")
                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean {
                Log.d(TAG, "[JS Alert] $message")
                result?.confirm()
                return true
            }
        }
    }

    private fun loadStremioWeb() {
        try {
            val webUrl = "file:///android_asset/$WEB_ASSET_PATH"
            Log.i(TAG, "Loading Stremio web from: $webUrl")
            loadUrl(webUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load Stremio web", e)
        }
    }

    private fun injectStremioAPI() {
        // The window.StremioCore object is now available globally due to addJavascriptInterface().
        // We now inject the window.stremioNative object, which wraps the JSON parsing logic.
        val jsCode = """
            (function() {
                if (window.StremioCore) {
                    window.stremioNative = {
                        getAddons: function() {
                            return JSON.parse(StremioCore.getAddons());
                        },
                        getLibrary: function() {
                            return JSON.parse(StremioCore.getLibrary());
                        },
                        search: function(query) {
                            return JSON.parse(StremioCore.search(query));
                        },
                        dispatchAction: function(action, payload) {
                            // payload must be stringified before sending to Kotlin bridge
                            var payloadString = JSON.stringify(payload || {});
                            var result = StremioCore.dispatchAction(action, payloadString);
                            return JSON.parse(result);
                        },
                        getSkipIntroData: function(itemId, duration) {
                            return JSON.parse(StremioCore.getSkipIntroData(itemId, duration));
                        },
                        invokeAddon: function(addonId, method, args) {
                            // args is already a JSON string from the web app
                            return JSON.parse(StremioCore.invokeAddon(addonId, method, args));
                        }
                    };
                    console.log('Stremio Native API injected and ready.');
                } else {
                    console.error('StremioCore bridge not found in WebView.');
                }
            })();
        """.trimIndent()
        evaluateJavascript(jsCode) { result ->
            Log.d(TAG, "API injection result: $result")
        }
    }

    // This function is now redundant, as the bridge logic is handled in StremioCoreBridge
    // and the stremioNative wrapper.
    // fun injectStremioCore(addonId: String, method: String, args: String) { ... }
    
    // We can remove it or leave it commented out. For a clean compile, let's remove it.
}

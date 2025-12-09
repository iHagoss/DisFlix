
package com.stremio.app

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.ConsoleMessage
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError

class StremioWebView(context: Context) : WebView(context) {
    companion object {
        private const val TAG = "StremioWebView"
        private const val WEB_ASSET_PATH = "web/index.html"
    }

    init {
        configureWebView()
        // The API bridge must be added BEFORE the web page loads
        addJavascriptInterface(StremioCoreBridge(), "StremioCore")
        addJavascriptInterface(StremioNativeAPI(), "Android")
        loadStremioWeb()
    }

    // Inner class to bridge JavaScript calls to Kotlin/JNI functions in StremioCore
    private inner class StremioCoreBridge {
        
        @JavascriptInterface
        fun isNativeAvailable(): Boolean {
            return StremioCore.isLibraryLoaded()
        }
        
        @JavascriptInterface
        fun getAddons(): String {
            Log.d(TAG, "JS -> getAddons()")
            return StremioCore.getAddons()
        }

        @JavascriptInterface
        fun getLibrary(): String {
            Log.d(TAG, "JS -> getLibrary()")
            return StremioCore.getLibrary()
        }

        @JavascriptInterface
        fun search(query: String): String {
            Log.d(TAG, "JS -> search($query)")
            return StremioCore.search(query)
        }

        @JavascriptInterface
        fun dispatchAction(action: String, payload: String): String {
            Log.d(TAG, "JS -> dispatchAction($action, ...)")
            return StremioCore.dispatchAction(action, payload)
        }

        @JavascriptInterface
        fun getSkipIntroData(itemId: String, duration: Long): String {
            Log.d(TAG, "JS -> getSkipIntroData($itemId, $duration)")
            return StremioCore.getSkipIntroData(itemId, duration)
        }
        
        @JavascriptInterface
        fun invokeAddon(addonId: String, method: String, args: String): String {
            Log.d(TAG, "JS -> invokeAddon($addonId, $method, ...)")
            return StremioCore.invokeAddon(addonId, method, args)
        }
    }
    
    // Native Android API exposed to JavaScript
    private inner class StremioNativeAPI {
        @JavascriptInterface
        fun log(message: String) {
            Log.d(TAG, "[JS] $message")
        }
        
        @JavascriptInterface
        fun getPlatform(): String {
            return "android"
        }
        
        @JavascriptInterface
        fun getVersion(): String {
            return "1.0.0"
        }
    }

    private fun configureWebView() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = true 
            allowFileAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            userAgentString = "Stremio/Android 1.0"
            cacheMode = WebSettings.LOAD_DEFAULT
            
            // Enable additional features
            setGeolocationEnabled(false)
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
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

            override fun onReceivedError(
                view: WebView?, 
                request: WebResourceRequest?, 
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView error: ${request?.url} - ${error?.description}")
            }
        }

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    Log.d(TAG, "[JS Console] ${it.message()} (${it.sourceId()}:${it.lineNumber()})")
                }
                return true
            }

            override fun onJsAlert(
                view: WebView?, 
                url: String?, 
                message: String?, 
                result: android.webkit.JsResult?
            ): Boolean {
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
        // Inject additional helper functions into the web context
        evaluateJavascript("""
            (function() {
                if (window.StremioNativeAPI) {
                    console.log('[Native] API already initialized');
                    return;
                }
                
                window.StremioNativeAPI = {
                    isNativeAvailable: function() {
                        return typeof window.StremioCore !== 'undefined' && 
                               window.StremioCore.isNativeAvailable();
                    },
                    log: function(message) {
                        if (window.Android) {
                            window.Android.log(message);
                        }
                    },
                    getPlatform: function() {
                        return window.Android ? window.Android.getPlatform() : 'web';
                    },
                    getVersion: function() {
                        return window.Android ? window.Android.getVersion() : '0.0.0';
                    }
                };
                
                console.log('[Native] StremioNativeAPI initialized');
            })();
        """.trimIndent(), null)
    }
}

package com.stremio.app

import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.util.Log

class StremioWebView(context: Context) : WebView(context) {
    companion object {
        private const val TAG = "StremioWebView"
        private const val WEB_ASSET_PATH = "web/index.html"
    }

    init {
        configureWebView()
        loadStremioWeb()
    }

    private fun configureWebView() {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = MIXED_CONTENT_ALLOW_ALL
            userAgentString = "Stremio/Android"
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
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
        val jsCode = """
            (function() {
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
                        return JSON.parse(StremioCore.dispatchAction(action, JSON.stringify(payload || {})));
                    },
                    getSkipIntroData: function(itemId, duration) {
                        return JSON.parse(StremioCore.getSkipIntroData(itemId, duration));
                    }
                };
                console.log('Stremio Native API injected');
            })();
        """.trimIndent()
        evaluateJavascript(jsCode) { result ->
            Log.d(TAG, "API injection result: $result")
        }
    }

    fun injectStremioCore(addonId: String, method: String, args: String) {
        val js = """
            window.stremioCore = {
                invoke: function(addonId, method, args) {
                    return StremioCore.$method(JSON.parse(args));
                }
            };
        """.trimIndent()
        evaluateJavascript(js, null)
    }
}

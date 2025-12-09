package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

/**
 * MainActivity - Entry point for Stremio Android application
 *
 * This activity hosts a WebView that loads the Stremio Web UI and bridges
 * communication between the JavaScript frontend and the native Rust core.
 *
 * Architecture:
 * - WebView loads Stremio Web UI from app/src/main/assets/web/
 * - JavaScript Bridge (WebAppInterface) enables JS ↔ Native communication
 * - StremioCore manages the Rust core library and addon synchronization
 * - User authentication syncs addons from Stremio API (account-based, not device-based)
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var webView: WebView

    /**
     * Initializes the activity, sets up WebView and loads Stremio Web UI
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
            Log.d(TAG, "WebView debugging enabled")
        }

        if (StremioCore.initialize(this)) {
            Log.d(TAG, "StremioCore initialized successfully")
        } else {
            Log.w(TAG, "StremioCore running in fallback mode (no native library)")
        }

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        setupWebView()
        loadStremioWeb()

        Log.d(TAG, "WebView version: ${getWebViewVersion()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        StremioCore.shutdown()
        Log.i(TAG, "MainActivity destroyed, StremioCore shutdown")
    }

    /**
     * Configures WebView settings and establishes JavaScript bridge
     *
     * Settings enabled:
     * - JavaScript execution (required for Stremio Web)
     * - DOM storage (for web app state persistence)
     * - Database access (for IndexedDB/localStorage)
     * - File access (for loading local web assets)
     *
     * The JavaScript interface "Android" is exposed to the web context,
     * allowing the Stremio Web UI to call native Android methods.
     */
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mediaPlaybackRequiresUserGesture = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")

                view?.evaluateJavascript(
                    """
                    if (window.StremioBridge) {
                        window.StremioBridge.init();
                        console.log('[MainActivity] StremioBridge initialized');
                    } else {
                        console.error('[MainActivity] StremioBridge not found');
                    }
                    """.trimIndent()
                ) { result ->
                    Log.d(TAG, "Bridge init result: $result")
                }
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                Log.e(TAG, "WebView error: ${error?.description} for ${request?.url}")
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val level = when (it.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> "ERROR"
                        ConsoleMessage.MessageLevel.WARNING -> "WARN"
                        else -> "INFO"
                    }
                    Log.d(TAG, "[WebView $level] ${it.message()} -- ${it.sourceId()}:${it.lineNumber()}")
                }
                return true
            }
        }
    }

    /**
     * Loads the Stremio Web UI from local assets
     *
     * The web UI is bundled in app/src/main/assets/web/ and includes:
     * - index.html - Main entry point
     * - scripts/bridge.js - JavaScript bridge initialization
     * - scripts/main.js - Stremio Web application code
     * - binaries/stremio_core_web_bg.wasm - Rust core compiled to WebAssembly
     *
     * Once loaded, the web UI will attempt to authenticate and sync addons
     * from the user's Stremio account (addons are account-based, not device-based)
     */
    private fun loadStremioWeb() {
        webView.loadUrl("file:///android_asset/web/index.html")
    }

    fun onBridgeReady() {
        Log.i(TAG, "Stremio Bridge is ready and initialized")
        runOnUiThread {
        }
    }

    fun navigateToPath(path: String) {
        Log.d(TAG, "Navigating to path: $path")
        webView.evaluateJavascript(
            """
            if (window.location) {
                window.location.hash = '$path';
            }
            """.trimIndent()
        ) { result ->
            Log.d(TAG, "Navigation result: $result")
        }
    }

    private fun getWebViewVersion(): String {
        return try {
            WebView(this).settings.userAgentString
        } catch (e: Exception) {
            "Unknown"
        }
    }
}

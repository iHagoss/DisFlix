package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivityRouter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable WebView debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
            Log.d(TAG, "WebView debugging enabled")
        }

        // Initialize StremioCore native library
        if (StremioCore.initCore()) {
            Log.d(TAG, "StremioCore initialized successfully")
        } else {
            Log.e(TAG, "Failed to initialize StremioCore - continuing without native core")
        }

        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        setupWebView()
        // injectBridgeSetup() // This function is now handled within onPageFinished

        // Log WebView info for debugging
        Log.d(TAG, "WebView version: ${getWebViewVersion()}")
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
        StremioCore.shutdown()
        Log.i(TAG, "MainActivity destroyed, StremioCore shutdown")
    }

    // Placeholder for WebView initialization and bridge setup
    private lateinit var webView: WebView

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

        // Add JavaScript interface BEFORE loading the page
        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started loading: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page loaded: $url")

                // Initialize the bridge after page loads
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

        // Load the web interface
        Log.d(TAG, "Loading web interface...")
        webView.loadUrl("file:///android_asset/web/index.html")
    }

    // This function is now called by the JavaScript bridge setup
    fun onBridgeReady() {
        Log.i(TAG, "Stremio Bridge is ready and initialized")
        runOnUiThread {
            // Bridge is ready, web app should now be functional
        }
    }

    private fun openPlayer(streamUrl: String, title: String) {
        Log.d(TAG, "Opening player for: $title with URL: $streamUrl")
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_URL, streamUrl)
            putExtra(PlayerActivity.EXTRA_TITLE, title)
        }
        startActivity(intent)
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
}
package com.stremio.app

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: StremioWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Stremio Core
        try {
            StremioCore.initCore(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Create WebView
        webView = StremioWebView(this)
        setContentView(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        StremioCore.shutdown()
        webView.destroy()
    }
}

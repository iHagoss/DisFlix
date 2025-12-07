package com.stremio.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MobileMainActivity : AppCompatActivity() {
    private lateinit var webView: StremioWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Stremio Core initialization is handled by the main MainActivity (the Router).
        
        // Create the WebView, which is the core of the mobile UI
        webView = StremioWebView(this)
        setContentView(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stremio Core shutdown is handled by the main MainActivity (the Router).
        webView.destroy()
    }
}

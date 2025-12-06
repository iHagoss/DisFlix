package com.stremio.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TvMainActivity : AppCompatActivity() {
    private lateinit var webView: StremioWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Stremio Core initialization is now handled by the MainActivity (Router).
        
        webView = StremioWebView(this)
        setContentView(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stremio Core shutdown is now handled by the MainActivity (Router).
        webView.destroy()
    }
}

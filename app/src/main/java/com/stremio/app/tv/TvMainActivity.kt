package com.stremio.app.tv

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.stremio.app.StremioCore
import com.stremio.app.StremioWebView

class TvMainActivity : AppCompatActivity() {
    private lateinit var webView: StremioWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize for TV
        try {
            StremioCore.initCore(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        webView = StremioWebView(this)
        setContentView(webView)
    }

    override fun onDestroy() {
        super.onDestroy()
        StremioCore.shutdown()
        webView.destroy()
    }
}

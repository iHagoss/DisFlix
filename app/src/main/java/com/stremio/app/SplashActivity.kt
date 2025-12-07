package com.stremio.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.stremio.app.R
import com.stremio.app.StremioApplication

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 1500)
    }
    
    private fun navigateToNextScreen() {
        val app = application as StremioApplication
        
        val nextActivity = if (app.authRepository.isLoggedIn) {
            DiscoverActivity::class.java
        } else {
            LoginActivity::class.java
        }
        
        startActivity(Intent(this, nextActivity))
        finish()
    }
}

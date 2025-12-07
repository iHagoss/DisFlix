package com.stremio.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivityRouter"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Core Management: Start the background server service
        // This ensures StremioCore is initialized and kept alive by the service,
        // independent of the UI Activities.
        try {
            val serviceIntent = Intent(this, ServerService::class.java)
            startService(serviceIntent)
            Log.i(TAG, "ServerService started successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start ServerService.", e)
        }

        // 2. Launch the native UI flow through SplashActivity
        // This uses the native Android activities that connect to official Stremio servers
        Log.d(TAG, "Launching Native UI: SplashActivity")
        startActivity(Intent(this, SplashActivity::class.java))
        
        // Prevents the user from navigating back to this router screen
        finish() 
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // NOTE: We do NOT call StremioCore.shutdown() or stop the ServerService here.
        // The ServerService is intended to run as long as the application process is alive,
        // or until the service decides to shut itself down (e.g., when the app is manually closed 
        // from the task manager, or the core decides to stop).
        Log.d(TAG, "MainActivity (Router) destroyed. ServerService remains running.")
    }
}

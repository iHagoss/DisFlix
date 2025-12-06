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

        // 2. Device Routing: Determine the correct UI activity based on device features.
        // Check if the device is a television using the recommended feature check (PackageManager.FEATURE_LEANBACK)
        val isTelevision = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        val targetActivity = if (isTelevision) {
            Log.d(TAG, "Launching TV UI: TvMainActivity")
            TvMainActivity::class.java
        } else {
            Log.d(TAG, "Launching Mobile UI: MobileMainActivity")
            MobileMainActivity::class.java
        }

        // 3. Launch UI
        startActivity(Intent(this, targetActivity))
        
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

package com.stremio.app

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if the device is a television using the recommended feature check (PackageManager.FEATURE_LEANBACK)
        val isTelevision = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        val targetActivity = if (isTelevision) {
            TvMainActivity::class.java
        } else {
            MobileMainActivity::class.java
        }

        // Initialize Stremio Core once in this central entry point before the UI is loaded
        try {
            StremioCore.initCore(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        startActivity(Intent(this, targetActivity))
        finish() // Prevents the user from navigating back to this router screen
    }

    override fun onDestroy() {
        super.onDestroy()
        // Shut down the core here when the primary router activity is destroyed.
        // NOTE: In a multi-activity app, this might be too soon. For a safer approach, 
        // you might move core shutdown to the Application class's onTerminate 
        // or ensure it only happens when the application is truly exiting.
        StremioCore.shutdown()
    }
}

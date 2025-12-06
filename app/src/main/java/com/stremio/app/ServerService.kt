package com.stremio.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class ServerService : Service() {
    
    companion object {
        private const val TAG = "ServerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "stremio_server_channel"
        private const val CHANNEL_NAME = "Stremio Server"
    }
    
    private val binder = ServerBinder()
    private var isServerRunning = false
    
    inner class ServerBinder : Binder() {
        fun getService(): ServerService = this@ServerService
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "ServerService created")
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "ServerService started")
        
        // Starts the service in the foreground immediately
        startForeground(NOTIFICATION_ID, createNotification())
        // Start the core server logic
        startServer()
        
        // This ensures the service restarts if killed by the OS
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        stopServer()
        super.onDestroy()
        Log.d(TAG, "ServerService destroyed")
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                // IMPORTANCE_LOW hides the notification icon from the status bar but still keeps it persistent
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Stremio background server"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        // Launches the main activity when the notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE // Use FLAG_IMMUTABLE for modern Android versions
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Stremio")
            .setContentText("Server running in background")
            // Assuming R.drawable.ic_stremio_icon exists or using R.drawable.ic_launcher as placeholder
            .setSmallIcon(R.drawable.ic_launcher) 
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Makes the notification non-dismissible
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun startServer() {
        if (!isServerRunning) {
            try {
                // Initialize the native core using the Application Context
                StremioCore.initCore(applicationContext) 
                isServerRunning = true
                Log.i(TAG, "Stremio server started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server: ${e.message}")
            }
        }
    }
    
    private fun stopServer() {
        if (isServerRunning) {
            try {
                StremioCore.shutdown()
                isServerRunning = false
                Log.i(TAG, "Stremio server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop server: ${e.message}")
            }
        }
    }
    
    fun isRunning(): Boolean = isServerRunning
}

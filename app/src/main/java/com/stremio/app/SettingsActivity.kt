package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stremio.app.R
import com.stremio.app.StremioApplication
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "SettingsActivity"
    }
    
    private lateinit var userEmailText: TextView
    private lateinit var userStatusText: TextView
    private lateinit var loginButton: Button
    private lateinit var logoutButton: Button
    private lateinit var syncLibraryButton: Button
    private lateinit var hardwareDecodingSwitch: Switch
    private lateinit var bingeWatchingSwitch: Switch
    
    private val app by lazy { application as StremioApplication }
    private val authRepository by lazy { app.authRepository }
    private val libraryRepository by lazy { app.libraryRepository }
    private val addonManager by lazy { app.addonManager }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        initViews()
        setupListeners()
        updateUI()
    }
    
    private fun initViews() {
        userEmailText = findViewById(R.id.user_email_text)
        userStatusText = findViewById(R.id.user_status_text)
        loginButton = findViewById(R.id.login_button)
        logoutButton = findViewById(R.id.logout_button)
        syncLibraryButton = findViewById(R.id.sync_library_button)
        hardwareDecodingSwitch = findViewById(R.id.hardware_decoding_switch)
        bingeWatchingSwitch = findViewById(R.id.binge_watching_switch)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
    }
    
    private fun setupListeners() {
        loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        
        logoutButton.setOnClickListener {
            confirmLogout()
        }
        
        syncLibraryButton.setOnClickListener {
            syncAllData()
        }
        
        hardwareDecodingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, 
                "Hardware decoding ${if (isChecked) "enabled" else "disabled"}", 
                Toast.LENGTH_SHORT).show()
        }
        
        bingeWatchingSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this,
                "Binge watching ${if (isChecked) "enabled" else "disabled"}",
                Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateUI() {
        val user = authRepository.getUser()
        val profile = authRepository.getProfile()
        
        if (user != null) {
            userEmailText.text = user.email
            userStatusText.text = if (profile?.isPremium == true) "Premium" else "Free"
            loginButton.visibility = android.view.View.GONE
            logoutButton.visibility = android.view.View.VISIBLE
            syncLibraryButton.visibility = android.view.View.VISIBLE
        } else {
            userEmailText.text = "Not logged in"
            userStatusText.text = ""
            loginButton.visibility = android.view.View.VISIBLE
            logoutButton.visibility = android.view.View.GONE
            syncLibraryButton.visibility = android.view.View.GONE
        }
        
        hardwareDecodingSwitch.isChecked = true
        bingeWatchingSwitch.isChecked = true
    }
    
    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun performLogout() {
        lifecycleScope.launch {
            authRepository.logout()
            Toast.makeText(this@SettingsActivity, "Logged out successfully", Toast.LENGTH_SHORT).show()
            updateUI()
        }
    }
    
    private fun syncAllData() {
        lifecycleScope.launch {
            Toast.makeText(this@SettingsActivity, "Syncing data...", Toast.LENGTH_SHORT).show()
            
            var hadError = false
            var errorMessage = ""
            
            try {
                Log.d(TAG, "Syncing addons...")
                val addonResult = addonManager.syncAddonsFromServer()
                addonResult.onSuccess { addons ->
                    Log.d(TAG, "Synced ${addons.size} addons")
                }.onFailure { error ->
                    Log.e(TAG, "Addon sync failed", error)
                    hadError = true
                    errorMessage = error.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Addon sync exception", e)
            }
            
            try {
                Log.d(TAG, "Syncing library...")
                val libraryResult = libraryRepository.syncLibrary()
                libraryResult.onSuccess {
                    Log.d(TAG, "Library sync completed, ${libraryRepository.getItemCount()} items")
                }.onFailure { error ->
                    Log.e(TAG, "Library sync failed", error)
                    hadError = true
                    errorMessage = error.message ?: "Unknown error"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Library sync exception", e)
            }
            
            if (hadError) {
                Toast.makeText(
                    this@SettingsActivity, 
                    "Sync completed with errors: $errorMessage", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@SettingsActivity, 
                    "All data synced successfully!", 
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

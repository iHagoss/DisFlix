package com.stremio.app

import android.app.Application
import android.util.Log
import com.stremio.app.addon.AddonManager
import com.stremio.app.data.repository.AuthRepository
import com.stremio.app.data.repository.LibraryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StremioApplication : Application() {
    
    companion object {
        private const val TAG = "StremioApplication"
        
        lateinit var instance: StremioApplication
            private set
    }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    lateinit var authRepository: AuthRepository
        private set
    
    lateinit var libraryRepository: LibraryRepository
        private set
    
    lateinit var addonManager: AddonManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        initializeComponents()
    }
    
    private fun initializeComponents() {
        Log.d(TAG, "Initializing Stremio components")
        
        authRepository = AuthRepository(this)
        libraryRepository = LibraryRepository(this, authRepository)
        addonManager = AddonManager(this)
        
        addonManager.setAuthRepository(authRepository)
        
        applicationScope.launch {
            try {
                addonManager.initialize()
                libraryRepository.initialize()
                Log.d(TAG, "Stremio components initialized successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize components", e)
            }
        }
    }
    
    fun reinitialize() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Reinitializing components after login/logout...")
                addonManager.syncAddonsFromServer()
                libraryRepository.syncLibrary()
                Log.d(TAG, "Reinitialize completed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reinitialize", e)
            }
        }
    }
    
    fun syncAllData() {
        applicationScope.launch {
            try {
                Log.d(TAG, "Syncing all data...")
                addonManager.syncAddonsFromServer()
                libraryRepository.syncLibrary()
                Log.d(TAG, "All data synced")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync all data", e)
            }
        }
    }
}

val Application.stremio: StremioApplication
    get() = this as StremioApplication

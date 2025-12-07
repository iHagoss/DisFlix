package com.stremio.app.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.stremio.app.api.ApiClient
import com.stremio.app.api.LoginRequest
import com.stremio.app.api.RegisterRequest
import com.stremio.app.api.LogoutRequest
import com.stremio.app.api.GetUserRequest
import com.stremio.app.api.AddonCollectionRequest
import com.stremio.app.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(private val context: Context) {
    
    companion object {
        private const val TAG = "AuthRepository"
        private const val PREFS_NAME = "stremio_auth"
        private const val KEY_AUTH_KEY = "auth_key"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER = "user"
        private const val KEY_PROFILE = "profile"
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private var currentProfile: Profile? = null
    
    val isLoggedIn: Boolean
        get() = prefs.getString(KEY_AUTH_KEY, null) != null
    
    val authKey: String?
        get() = prefs.getString(KEY_AUTH_KEY, null)
    
    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    
    fun getUser(): User? {
        val userJson = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(userJson, User::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getProfile(): Profile? {
        if (currentProfile != null) return currentProfile
        
        val profileJson = prefs.getString(KEY_PROFILE, null) ?: return null
        return try {
            gson.fromJson(profileJson, Profile::class.java).also {
                currentProfile = it
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun login(email: String, password: String): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.stremioApi.login(
                    LoginRequest(email = email, password = password)
                )
                
                if (response.isSuccessful && response.body()?.result != null) {
                    val authResult = response.body()!!.result!!
                    saveAuth(authResult.authKey, authResult.user)
                    
                    val addons = fetchAddons(authResult.authKey)
                    val profile = Profile(
                        auth = AuthInfo(authResult.authKey, authResult.user.id),
                        user = authResult.user,
                        addons = addons
                    )
                    saveProfile(profile)
                    
                    Result.success(profile)
                } else {
                    val error = response.body()?.error?.message ?: "Login failed"
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun register(email: String, password: String, gdprConsent: GDPRConsent): Result<Profile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = ApiClient.stremioApi.register(
                    RegisterRequest(email = email, password = password, gdprConsent = gdprConsent)
                )
                
                if (response.isSuccessful && response.body()?.result != null) {
                    val authResult = response.body()!!.result!!
                    saveAuth(authResult.authKey, authResult.user)
                    
                    val profile = Profile(
                        auth = AuthInfo(authResult.authKey, authResult.user.id),
                        user = authResult.user,
                        addons = emptyList()
                    )
                    saveProfile(profile)
                    
                    Result.success(profile)
                } else {
                    val error = response.body()?.error?.message ?: "Registration failed"
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Register error", e)
                Result.failure(e)
            }
        }
    }
    
    suspend fun logout(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val currentAuthKey = authKey
                if (currentAuthKey != null) {
                    ApiClient.stremioApi.logout(LogoutRequest(authKey = currentAuthKey))
                }
                clearAuth()
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Logout error", e)
                clearAuth()
                Result.success(Unit)
            }
        }
    }
    
    suspend fun refreshUser(): Result<User> {
        return withContext(Dispatchers.IO) {
            try {
                val currentAuthKey = authKey ?: return@withContext Result.failure(Exception("Not logged in"))
                
                val response = ApiClient.stremioApi.getUser(GetUserRequest(authKey = currentAuthKey))
                
                if (response.isSuccessful && response.body()?.result != null) {
                    val user = response.body()!!.result!!
                    saveUser(user)
                    Result.success(user)
                } else {
                    Result.failure(Exception("Failed to get user"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh user error", e)
                Result.failure(e)
            }
        }
    }
    
    private suspend fun fetchAddons(authKey: String): List<Addon> {
        return try {
            val response = ApiClient.stremioApi.getAddonCollection(
                AddonCollectionRequest(authKey = authKey, update = true)
            )
            
            if (response.isSuccessful && response.body()?.result != null) {
                response.body()!!.result!!.addons
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch addons error", e)
            emptyList()
        }
    }
    
    private fun saveAuth(authKey: String, user: User) {
        prefs.edit()
            .putString(KEY_AUTH_KEY, authKey)
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }
    
    private fun saveUser(user: User) {
        prefs.edit()
            .putString(KEY_USER, gson.toJson(user))
            .apply()
    }
    
    private fun saveProfile(profile: Profile) {
        currentProfile = profile
        prefs.edit()
            .putString(KEY_PROFILE, gson.toJson(profile))
            .apply()
    }
    
    private fun clearAuth() {
        currentProfile = null
        prefs.edit().clear().apply()
    }
}

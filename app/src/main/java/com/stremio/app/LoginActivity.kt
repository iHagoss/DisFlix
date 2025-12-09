package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.GDPRConsent
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var skipButton: TextView
    private lateinit var progressBar: ProgressBar
    
    private val app by lazy { application as StremioApplication }
    private val authRepository by lazy { app.authRepository }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        initViews()
        setupListeners()
    }
    
    private fun initViews() {
        emailInput = findViewById(R.id.email_input)
        passwordInput = findViewById(R.id.password_input)
        loginButton = findViewById(R.id.login_button)
        registerButton = findViewById(R.id.register_button)
        skipButton = findViewById(R.id.skip_button)
        progressBar = findViewById(R.id.progress_bar)
    }
    
    private fun setupListeners() {
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performLogin(email, password)
        }
        
        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            performRegister(email, password)
        }
        
        skipButton.setOnClickListener {
            navigateToDiscover()
        }
    }
    
    private fun performLogin(email: String, password: String) {
        setLoading(true)
        
        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            
            result.onSuccess { profile ->
                Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                
                app.reinitialize()
                
                navigateToDiscover()
            }.onFailure { error ->
                setLoading(false)
                Toast.makeText(this@LoginActivity, error.message ?: "Login failed", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun performRegister(email: String, password: String) {
        setLoading(true)
        
        val gdprConsent = GDPRConsent(
            marketing = false,
            privacy = true,
            tos = true,
            from = "android"
        )
        
        lifecycleScope.launch {
            val result = authRepository.register(email, password, gdprConsent)
            
            result.onSuccess {
                Toast.makeText(this@LoginActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                
                app.reinitialize()
                
                navigateToDiscover()
            }.onFailure { error ->
                setLoading(false)
                Toast.makeText(this@LoginActivity, error.message ?: "Registration failed", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        loginButton.isEnabled = !loading
        registerButton.isEnabled = !loading
        skipButton.isEnabled = !loading
        emailInput.isEnabled = !loading
        passwordInput.isEnabled = !loading
    }
    
    private fun navigateToDiscover() {
        startActivity(Intent(this, DiscoverActivity::class.java))
        finish()
    }
}

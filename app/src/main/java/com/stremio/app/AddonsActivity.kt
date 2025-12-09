package com.stremio.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.Addon
import com.stremio.app.ui.adapters.AddonAdapter
import kotlinx.coroutines.launch

class AddonsActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "AddonsActivity"
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var addAddonButton: Button
    private lateinit var emptyText: TextView
    
    private lateinit var adapter: AddonAdapter
    
    private val app by lazy { application as StremioApplication }
    private val addonManager by lazy { app.addonManager }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addons)
        
        initViews()
        setupRecyclerView()
        loadAddons()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        addAddonButton = findViewById(R.id.add_addon_button)
        emptyText = findViewById(R.id.empty_text)
        
        setSupportActionBar(findViewById(R.id.toolbar)) 
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Addons"
        
        addAddonButton.setOnClickListener {
            showAddAddonDialog()
        }
    }
    
    private fun setupRecyclerView() {
        adapter = AddonAdapter(
            onAddonClick = { addon ->
                showAddonDetails(addon)
            },
            onUninstallClick = { addon ->
                confirmUninstall(addon)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AddonsActivity)
            adapter = this@AddonsActivity.adapter
        }
    }
    
    private fun loadAddons() {
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                if (app.authRepository.isLoggedIn) {
                    Log.d(TAG, "User is logged in, syncing addons from server...")
                    val result = addonManager.syncAddonsFromServer()
                    result.onFailure { e ->
                        Log.e(TAG, "Failed to sync addons from server", e)
                    }
                }
                
                val addons = addonManager.getInstalledAddons()
                Log.d(TAG, "Loaded ${addons.size} addons")
                
                progressBar.visibility = View.GONE
                
                if (addons.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No addons installed.\nAdd an addon to start browsing content."
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(addons)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading addons", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this@AddonsActivity, "Failed to load addons", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddAddonDialog() {
        val input = EditText(this).apply {
            hint = "Enter addon manifest URL"
            setPadding(48, 32, 48, 32) 
        }
        
        AlertDialog.Builder(this)
            .setTitle("Add Addon")
            .setMessage("Enter the addon manifest URL (must end with /manifest.json)")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val url = input.text.toString().trim()
                if (url.isNotEmpty()) {
                    installAddon(url)
                } else {
                    Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun installAddon(url: String) {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val success = addonManager.installAddon(url)
            
            progressBar.visibility = View.GONE
            
            if (success) {
                Toast.makeText(this@AddonsActivity, "Addon installed successfully", Toast.LENGTH_SHORT).show()
                loadAddons()
            } else {
                Toast.makeText(this@AddonsActivity, "Failed to install addon", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun showAddonDetails(addon: Addon) {
        val manifest = addon.manifest
        
        val message = buildString {
            appendLine("Name: ${manifest.name}")
            appendLine("ID: ${manifest.id}")
            appendLine("Version: ${manifest.version}")
            appendLine()
            manifest.description?.let { appendLine("Description: $it\n") }
            appendLine("Types: ${manifest.types.joinToString(", ")}")
            appendLine("Resources: ${manifest.resources.joinToString(", ") { it.name }}")
            appendLine()
            appendLine("Catalogs: ${manifest.catalogs.size}")
            if (addon.flags.official) appendLine("\nOfficial Stremio Addon")
        }
        
        AlertDialog.Builder(this)
            .setTitle(manifest.name)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun confirmUninstall(addon: Addon) {
        if (addon.flags.official) {
            Toast.makeText(this, "Cannot uninstall official addons", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle("Uninstall Addon")
            .setMessage("Are you sure you want to uninstall ${addon.manifest.name}?")
            .setPositiveButton("Uninstall") { _, _ ->
                addonManager.uninstallAddon(addon.manifest.id)
                loadAddons()
                Toast.makeText(this, "Addon uninstalled", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

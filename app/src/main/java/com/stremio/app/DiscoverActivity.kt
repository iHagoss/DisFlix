package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.DiscoverRow
import com.stremio.app.data.models.MetaItemPreview
import com.stremio.app.ui.adapters.DiscoverAdapter
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class DiscoverActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "DiscoverActivity"
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var emptyText: TextView
    
    private lateinit var discoverAdapter: DiscoverAdapter
    
    private val app by lazy { application as StremioApplication }
    private val addonManager by lazy { app.addonManager }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_discover)
        
        initViews()
        setupRecyclerView()
        setupBottomNavigation()
        loadDiscoverContent()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        bottomNav = findViewById(R.id.bottom_navigation)
        emptyText = findViewById(R.id.empty_text)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.title = "Discover"
    }
    
    private fun setupRecyclerView() {
        discoverAdapter = DiscoverAdapter { item ->
            openDetail(item)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@DiscoverActivity)
            adapter = discoverAdapter
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_discover -> true
                R.id.nav_library -> {
                    startActivity(Intent(this, LibraryActivity::class.java))
                    true
                }
                R.id.nav_search -> {
                    startActivity(Intent(this, SearchActivity::class.java))
                    true
                }
                R.id.nav_addons -> {
                    startActivity(Intent(this, AddonsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadDiscoverContent() {
        progressBar.visibility = View.VISIBLE
        emptyText.visibility = View.GONE
        recyclerView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val installedAddons = addonManager.getInstalledAddons()
                Log.d(TAG, "Installed addons: ${installedAddons.size}")
                
                if (installedAddons.isEmpty()) {
                    Log.d(TAG, "No addons installed, trying to sync...")
                    addonManager.syncAddonsFromServer()
                }
                
                val rows = mutableListOf<DiscoverRow>()
                
                val movieCatalogs = addonManager.getCatalogsForType("movie")
                Log.d(TAG, "Movie catalogs: ${movieCatalogs.size}")
                
                val movieRows = movieCatalogs.take(4).map { (addon, catalog) ->
                    async {
                        try {
                            val response = addonManager.getCatalog(addon, catalog)
                            if (response != null && response.metas.isNotEmpty()) {
                                Log.d(TAG, "Loaded catalog ${catalog.id}: ${response.metas.size} items")
                                DiscoverRow(
                                    title = catalog.name ?: "${addon.manifest.name} - Movies",
                                    items = response.metas.take(20),
                                    catalog = catalog,
                                    addon = addon,
                                    deepLink = "stremio:///discover/${addon.manifest.id}/${catalog.type}/${catalog.id}"
                                )
                            } else {
                                Log.d(TAG, "Catalog ${catalog.id} returned empty")
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading catalog ${catalog.id}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
                
                rows.addAll(movieRows)
                
                val seriesCatalogs = addonManager.getCatalogsForType("series")
                Log.d(TAG, "Series catalogs: ${seriesCatalogs.size}")
                
                val seriesRows = seriesCatalogs.take(4).map { (addon, catalog) ->
                    async {
                        try {
                            val response = addonManager.getCatalog(addon, catalog)
                            if (response != null && response.metas.isNotEmpty()) {
                                Log.d(TAG, "Loaded catalog ${catalog.id}: ${response.metas.size} items")
                                DiscoverRow(
                                    title = catalog.name ?: "${addon.manifest.name} - Series",
                                    items = response.metas.take(20),
                                    catalog = catalog,
                                    addon = addon,
                                    deepLink = "stremio:///discover/${addon.manifest.id}/${catalog.type}/${catalog.id}"
                                )
                            } else {
                                null
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading catalog ${catalog.id}", e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
                
                rows.addAll(seriesRows)
                
                Log.d(TAG, "Total discover rows: ${rows.size}")
                
                progressBar.visibility = View.GONE
                
                if (rows.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No content available.\nPlease install some addons."
                    recyclerView.visibility = View.GONE
                } else {
                    emptyText.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    discoverAdapter.submitList(rows)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load discover content", e)
                progressBar.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                emptyText.text = "Failed to load content.\nPlease try again."
                Toast.makeText(this@DiscoverActivity, "Failed to load content: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun openDetail(item: MetaItemPreview) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_META_ID, item.id)
            putExtra(DetailActivity.EXTRA_META_TYPE, item.type)
            putExtra(DetailActivity.EXTRA_META_NAME, item.name)
            putExtra(DetailActivity.EXTRA_META_POSTER, item.poster)
        }
        startActivity(intent)
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_discover, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_search -> {
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            R.id.action_refresh -> {
                loadDiscoverContent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_discover
    }
}

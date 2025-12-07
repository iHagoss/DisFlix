package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
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
import kotlinx.coroutines.launch

class DiscoverActivity : AppCompatActivity() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var bottomNav: BottomNavigationView
    
    private lateinit var discoverAdapter: DiscoverAdapter
    
    private val addonManager by lazy { (application as StremioApplication).addonManager }
    
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
        
        lifecycleScope.launch {
            try {
                val rows = mutableListOf<DiscoverRow>()
                
                val movieCatalogs = addonManager.getCatalogsForType("movie")
                for ((addon, catalog) in movieCatalogs.take(3)) {
                    val response = addonManager.getCatalog(addon, catalog)
                    if (response != null && response.metas.isNotEmpty()) {
                        rows.add(DiscoverRow(
                            title = catalog.name ?: "${addon.manifest.name} - Movies",
                            items = response.metas,
                            catalog = catalog,
                            addon = addon,
                            deepLink = "stremio:///discover/${addon.manifest.id}/${catalog.type}/${catalog.id}"
                        ))
                    }
                }
                
                val seriesCatalogs = addonManager.getCatalogsForType("series")
                for ((addon, catalog) in seriesCatalogs.take(3)) {
                    val response = addonManager.getCatalog(addon, catalog)
                    if (response != null && response.metas.isNotEmpty()) {
                        rows.add(DiscoverRow(
                            title = catalog.name ?: "${addon.manifest.name} - Series",
                            items = response.metas,
                            catalog = catalog,
                            addon = addon,
                            deepLink = "stremio:///discover/${addon.manifest.id}/${catalog.type}/${catalog.id}"
                        ))
                    }
                }
                
                discoverAdapter.submitList(rows)
                progressBar.visibility = View.GONE
                
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@DiscoverActivity, "Failed to load content", Toast.LENGTH_SHORT).show()
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
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_discover
    }
}

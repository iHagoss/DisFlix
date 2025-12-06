package com.stremio.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.LibraryItem
import com.stremio.app.data.models.MetaItemPreview
import com.stremio.app.data.models.PosterShape
import com.stremio.app.ui.adapters.MetaPreviewAdapter
import kotlinx.coroutines.launch

class LibraryActivity : AppCompatActivity() {
    
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    
    private lateinit var adapter: MetaPreviewAdapter
    
    private val libraryRepository by lazy { (application as StremioApplication).libraryRepository }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library)
        
        initViews()
        setupTabs()
        setupRecyclerView()
        loadLibrary("all")
    }
    
    private fun initViews() {
        tabLayout = findViewById(R.id.tab_layout)
        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        emptyText = findViewById(R.id.empty_text)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Library"
    }
    
    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("All"))
        tabLayout.addTab(tabLayout.newTab().setText("Movies"))
        tabLayout.addTab(tabLayout.newTab().setText("Series"))
        tabLayout.addTab(tabLayout.newTab().setText("Continue"))
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadLibrary("all")
                    1 -> loadLibrary("movie")
                    2 -> loadLibrary("series")
                    3 -> loadContinueWatching()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupRecyclerView() {
        adapter = MetaPreviewAdapter { item ->
            openDetail(item)
        }
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@LibraryActivity, 3)
            adapter = this@LibraryActivity.adapter
        }
    }
    
    private fun loadLibrary(type: String) {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val items = if (type == "all") {
                libraryRepository.getAllItems()
            } else {
                libraryRepository.getItemsByType(type)
            }
            
            displayItems(items)
        }
    }
    
    private fun loadContinueWatching() {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val items = libraryRepository.getContinueWatching()
            displayItems(items)
        }
    }
    
    private fun displayItems(items: List<LibraryItem>) {
        progressBar.visibility = View.GONE
        
        if (items.isEmpty()) {
            emptyText.visibility = View.VISIBLE
            emptyText.text = "Your library is empty"
            recyclerView.visibility = View.GONE
        } else {
            emptyText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            
            val previews = items.map { item ->
                MetaItemPreview(
                    id = item.id,
                    type = item.type,
                    name = item.name,
                    poster = item.poster,
                    posterShape = item.posterShape
                )
            }
            
            adapter.submitList(previews)
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

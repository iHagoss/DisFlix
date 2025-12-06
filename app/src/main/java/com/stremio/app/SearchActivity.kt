package com.stremio.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.MetaItemPreview
import com.stremio.app.ui.adapters.MetaPreviewAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {
    
    private lateinit var searchInput: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    
    private lateinit var adapter: MetaPreviewAdapter
    
    private val addonManager by lazy { (application as StremioApplication).addonManager }
    
    private var searchJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        
        initViews()
        setupRecyclerView()
        setupSearchInput()
    }
    
    private fun initViews() {
        searchInput = findViewById(R.id.search_input)
        recyclerView = findViewById(R.id.recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        emptyText = findViewById(R.id.empty_text)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Search"
    }
    
    private fun setupRecyclerView() {
        adapter = MetaPreviewAdapter { item ->
            openDetail(item)
        }
        
        recyclerView.apply {
            layoutManager = GridLayoutManager(this@SearchActivity, 3)
            adapter = this@SearchActivity.adapter
        }
    }
    
    private fun setupSearchInput() {
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchInput.text.toString())
                true
            } else {
                false
            }
        }
        
        searchInput.requestFocus()
    }
    
    private fun performSearch(query: String) {
        if (query.length < 2) return
        
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(300)
            
            progressBar.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            
            try {
                val results = addonManager.search(query)
                
                val allItems = results.flatMap { it.items }.distinctBy { it.id }
                
                if (allItems.isEmpty()) {
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = "No results found for \"$query\""
                } else {
                    emptyText.visibility = View.GONE
                }
                
                adapter.submitList(allItems)
                
            } catch (e: Exception) {
                emptyText.visibility = View.VISIBLE
                emptyText.text = "Search failed. Please try again."
            } finally {
                progressBar.visibility = View.GONE
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
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

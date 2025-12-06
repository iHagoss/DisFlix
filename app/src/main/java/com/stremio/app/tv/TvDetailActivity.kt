package com.stremio.app.tv

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.squareup.picasso.Picasso
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.StremioCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TvDetailActivity : FragmentActivity() {
    
    companion object {
        private const val TAG = "TvDetailActivity"
        const val EXTRA_META_ID = "meta_id"
        const val EXTRA_META_TYPE = "meta_type"
        const val EXTRA_TITLE = "title"
        const val EXTRA_POSTER = "poster"
        const val EXTRA_DESCRIPTION = "description"
    }
    
    private var metaId: String? = null
    private var metaType: String? = null
    
    private lateinit var titleView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var posterView: ImageView
    private lateinit var playButton: Button
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_detail)
        
        metaId = intent.getStringExtra(EXTRA_META_ID)
        metaType = intent.getStringExtra(EXTRA_META_TYPE)
        
        try {
            StremioCore.initCore(this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init StremioCore", e)
        }
        
        initViews()
        loadMetaDetails()
    }
    
    private fun initViews() {
        titleView = findViewById(R.id.title_text)
        descriptionView = findViewById(R.id.description_text)
        posterView = findViewById(R.id.poster_image)
        playButton = findViewById(R.id.play_button)
        progressBar = findViewById(R.id.progress_bar)
        
        playButton.setOnClickListener {
            launchPlayer()
        }
        
        playButton.visibility = View.GONE
    }
    
    private fun loadMetaDetails() {
        progressBar.visibility = View.VISIBLE
        
        val title = intent.getStringExtra(EXTRA_TITLE)
        val poster = intent.getStringExtra(EXTRA_POSTER)
        val description = intent.getStringExtra(EXTRA_DESCRIPTION)
        
        if (!title.isNullOrEmpty()) {
            titleView.text = title
        }
        
        if (!description.isNullOrEmpty()) {
            descriptionView.text = description
        }
        
        if (!poster.isNullOrEmpty()) {
            Picasso.get()
                .load(poster)
                .placeholder(R.drawable.ic_launcher)
                .error(R.drawable.ic_launcher)
                .into(posterView)
        }
        
        if (metaId != null && metaType != null) {
            loadFromRepository()
        } else {
            progressBar.visibility = View.GONE
            if (title.isNullOrEmpty()) {
                titleView.text = getString(R.string.no_content)
            }
        }
    }
    
    private fun loadFromRepository() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = StremioApplication.instance
                val meta = app.addonManager.getMetaItem(metaType!!, metaId!!)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (meta != null) {
                        titleView.text = meta.name ?: "Unknown"
                        descriptionView.text = meta.description ?: ""
                        
                        meta.poster?.let { posterUrl ->
                            Picasso.get()
                                .load(posterUrl)
                                .placeholder(R.drawable.ic_launcher)
                                .error(R.drawable.ic_launcher)
                                .into(posterView)
                        }
                        
                        playButton.visibility = View.VISIBLE
                    } else {
                        titleView.text = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.no_content)
                        playButton.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load meta: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    playButton.visibility = View.VISIBLE
                }
            }
        }
    }
    
    private fun launchPlayer() {
        val intent = Intent(this, TvPlayerActivity::class.java).apply {
            putExtra(TvPlayerActivity.EXTRA_ITEM_ID, metaId)
        }
        startActivity(intent)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        StremioCore.shutdown()
    }
}

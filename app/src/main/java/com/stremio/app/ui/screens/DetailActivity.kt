package com.stremio.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.stremio.app.PlayerActivity
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.MetaItem
import com.stremio.app.data.models.Stream
import com.stremio.app.data.models.Video
import com.stremio.app.ui.adapters.VideoAdapter
import com.stremio.app.ui.adapters.StreamAdapter
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_META_ID = "meta_id"
        const val EXTRA_META_TYPE = "meta_type"
        const val EXTRA_META_NAME = "meta_name"
        const val EXTRA_META_POSTER = "meta_poster"
    }
    
    private lateinit var posterImage: ImageView
    private lateinit var backgroundImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var releaseInfoText: TextView
    private lateinit var runtimeText: TextView
    private lateinit var playButton: Button
    private lateinit var addToLibraryButton: Button
    private lateinit var videosRecyclerView: RecyclerView
    private lateinit var streamsRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var videosLabel: TextView
    private lateinit var streamsLabel: TextView
    
    private var metaItem: MetaItem? = null
    private var selectedVideo: Video? = null
    private var streams: List<Pair<com.stremio.app.data.models.Addon, Stream>> = emptyList()
    
    private val addonManager by lazy { (application as StremioApplication).addonManager }
    private val libraryRepository by lazy { (application as StremioApplication).libraryRepository }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        
        initViews()
        loadInitialData()
        loadMetaDetails()
    }
    
    private fun initViews() {
        posterImage = findViewById(R.id.poster_image)
        backgroundImage = findViewById(R.id.background_image)
        titleText = findViewById(R.id.title_text)
        descriptionText = findViewById(R.id.description_text)
        releaseInfoText = findViewById(R.id.release_info_text)
        runtimeText = findViewById(R.id.runtime_text)
        playButton = findViewById(R.id.play_button)
        addToLibraryButton = findViewById(R.id.add_to_library_button)
        videosRecyclerView = findViewById(R.id.videos_recycler_view)
        streamsRecyclerView = findViewById(R.id.streams_recycler_view)
        progressBar = findViewById(R.id.progress_bar)
        videosLabel = findViewById(R.id.videos_label)
        streamsLabel = findViewById(R.id.streams_label)
        
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        playButton.setOnClickListener {
            playContent()
        }
        
        addToLibraryButton.setOnClickListener {
            toggleLibrary()
        }
    }
    
    private fun loadInitialData() {
        val name = intent.getStringExtra(EXTRA_META_NAME) ?: ""
        val poster = intent.getStringExtra(EXTRA_META_POSTER)
        
        titleText.text = name
        supportActionBar?.title = name
        
        poster?.let {
            Picasso.get()
                .load(it)
                .placeholder(R.drawable.placeholder_poster)
                .into(posterImage)
        }
    }
    
    private fun loadMetaDetails() {
        val metaId = intent.getStringExtra(EXTRA_META_ID) ?: return
        val metaType = intent.getStringExtra(EXTRA_META_TYPE) ?: return
        
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val metaAddons = addonManager.getAddonsForResource("meta", metaType)
                
                for (addon in metaAddons) {
                    val meta = addonManager.getMeta(addon, metaType, metaId)
                    if (meta != null) {
                        metaItem = meta
                        displayMetaItem(meta)
                        break
                    }
                }
                
                loadStreams(metaType, metaId)
                updateLibraryButton()
                
            } catch (e: Exception) {
                Toast.makeText(this@DetailActivity, "Failed to load details", Toast.LENGTH_SHORT).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun displayMetaItem(meta: MetaItem) {
        titleText.text = meta.name
        supportActionBar?.title = meta.name
        
        descriptionText.text = meta.description ?: ""
        releaseInfoText.text = meta.releaseInfo ?: ""
        runtimeText.text = meta.runtime ?: ""
        
        meta.poster?.let {
            Picasso.get()
                .load(it)
                .placeholder(R.drawable.placeholder_poster)
                .into(posterImage)
        }
        
        meta.background?.let {
            Picasso.get()
                .load(it)
                .into(backgroundImage)
        }
        
        if (meta.videos.isNotEmpty()) {
            videosLabel.visibility = View.VISIBLE
            videosRecyclerView.visibility = View.VISIBLE
            
            val videoAdapter = VideoAdapter { video ->
                selectedVideo = video
                loadStreamsForVideo(meta.type, meta.id, video.id)
            }
            
            videosRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@DetailActivity)
                adapter = videoAdapter
            }
            
            videoAdapter.submitList(meta.videos)
            
            if (meta.videos.size == 1) {
                selectedVideo = meta.videos.first()
            }
        } else {
            videosLabel.visibility = View.GONE
            videosRecyclerView.visibility = View.GONE
        }
    }
    
    private fun loadStreams(type: String, id: String, videoId: String? = null) {
        lifecycleScope.launch {
            try {
                streams = addonManager.getStreams(type, id, videoId)
                displayStreams()
            } catch (e: Exception) {
                Toast.makeText(this@DetailActivity, "Failed to load streams", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun loadStreamsForVideo(type: String, id: String, videoId: String) {
        progressBar.visibility = View.VISIBLE
        loadStreams(type, id, videoId)
    }
    
    private fun displayStreams() {
        progressBar.visibility = View.GONE
        
        if (streams.isNotEmpty()) {
            streamsLabel.visibility = View.VISIBLE
            streamsRecyclerView.visibility = View.VISIBLE
            playButton.visibility = View.VISIBLE
            
            val streamAdapter = StreamAdapter { addon, stream ->
                playStream(stream)
            }
            
            streamsRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@DetailActivity)
                adapter = streamAdapter
            }
            
            streamAdapter.submitList(streams)
        } else {
            streamsLabel.visibility = View.GONE
            streamsRecyclerView.visibility = View.GONE
            playButton.visibility = View.GONE
        }
    }
    
    private fun playContent() {
        val stream = streams.firstOrNull()?.second
        if (stream != null) {
            playStream(stream)
        } else {
            Toast.makeText(this, "No streams available", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun playStream(stream: Stream) {
        val url = stream.playableUrl
        if (url == null) {
            Toast.makeText(this, "Cannot play this stream", Toast.LENGTH_SHORT).show()
            return
        }
        
        val meta = metaItem
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_URL, url)
            putExtra(PlayerActivity.EXTRA_ITEM_ID, meta?.id ?: "")
        }
        startActivity(intent)
    }
    
    private fun toggleLibrary() {
        val meta = metaItem ?: return
        
        lifecycleScope.launch {
            val isInLibrary = libraryRepository.isInLibrary(meta.id)
            
            if (isInLibrary) {
                libraryRepository.removeFromLibrary(meta.id)
                Toast.makeText(this@DetailActivity, "Removed from library", Toast.LENGTH_SHORT).show()
            } else {
                libraryRepository.addToLibrary(meta.toPreview())
                Toast.makeText(this@DetailActivity, "Added to library", Toast.LENGTH_SHORT).show()
            }
            
            updateLibraryButton()
        }
    }
    
    private fun updateLibraryButton() {
        val meta = metaItem ?: return
        val isInLibrary = libraryRepository.isInLibrary(meta.id)
        
        addToLibraryButton.text = if (isInLibrary) "Remove from Library" else "Add to Library"
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

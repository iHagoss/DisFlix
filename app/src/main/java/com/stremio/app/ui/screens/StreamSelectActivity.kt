package com.stremio.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.stremio.app.PlayerActivity
import com.stremio.app.R
import com.stremio.app.StremioApplication
import com.stremio.app.data.models.Stream
import com.stremio.app.stremio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StreamSelectActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "StreamSelectActivity"
        const val EXTRA_META_ID = "meta_id"
        const val EXTRA_META_TYPE = "meta_type"
        const val EXTRA_VIDEO_ID = "video_id"
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyView: TextView
    
    private var metaId: String? = null
    private var metaType: String? = null
    private var videoId: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stream_select)
        
        recyclerView = findViewById(R.id.streams_recycler)
        progressBar = findViewById(R.id.progress_bar)
        emptyView = findViewById(R.id.empty_view)
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        metaId = intent.getStringExtra(EXTRA_META_ID)
        metaType = intent.getStringExtra(EXTRA_META_TYPE)
        videoId = intent.getStringExtra(EXTRA_VIDEO_ID)
        
        if (metaId != null && metaType != null) {
            loadStreams()
        } else {
            showEmpty("Missing content information")
        }
    }
    
    private fun loadStreams() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyView.visibility = View.GONE
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = application.stremio
                val streams = app.addonManager.getStreams(metaType!!, metaId!!, videoId)
                
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    if (streams.isNotEmpty()) {
                        recyclerView.visibility = View.VISIBLE
                        recyclerView.adapter = StreamAdapter(streams) { stream ->
                            playStream(stream)
                        }
                    } else {
                        showEmpty("No streams found")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load streams: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showEmpty("Failed to load streams")
                }
            }
        }
    }
    
    private fun showEmpty(message: String) {
        emptyView.text = message
        emptyView.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
    
    private fun playStream(stream: Stream) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(PlayerActivity.EXTRA_STREAM_URL, stream.url ?: stream.externalUrl)
            putExtra(PlayerActivity.EXTRA_ITEM_ID, metaId)
        }
        startActivity(intent)
    }
    
    private inner class StreamAdapter(
        private val streams: List<Stream>,
        private val onClick: (Stream) -> Unit
    ) : RecyclerView.Adapter<StreamAdapter.ViewHolder>() {
        
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val titleText: TextView = view.findViewById(R.id.stream_title)
            val subtitleText: TextView = view.findViewById(R.id.stream_subtitle)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_stream, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val stream = streams[position]
            holder.titleText.text = stream.title ?: stream.name ?: "Stream ${position + 1}"
            holder.subtitleText.text = stream.description ?: stream.addonName ?: ""
            holder.itemView.setOnClickListener { onClick(stream) }
        }
        
        override fun getItemCount() = streams.size
    }
}

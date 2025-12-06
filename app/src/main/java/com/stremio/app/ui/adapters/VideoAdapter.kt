package com.stremio.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.stremio.app.R
import com.stremio.app.data.models.Video
import java.text.SimpleDateFormat
import java.util.Locale

class VideoAdapter(
    private val onVideoClick: (Video) -> Unit
) : ListAdapter<Video, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {
    
    private var selectedPosition = -1
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position), position == selectedPosition)
    }
    
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val thumbnailImage: ImageView = itemView.findViewById(R.id.thumbnail_image)
        private val titleText: TextView = itemView.findViewById(R.id.title_text)
        private val episodeText: TextView = itemView.findViewById(R.id.episode_text)
        private val releasedText: TextView = itemView.findViewById(R.id.released_text)
        private val overviewText: TextView = itemView.findViewById(R.id.overview_text)
        
        private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        
        fun bind(video: Video, isSelected: Boolean) {
            titleText.text = video.title
            
            val seriesInfo = video.seriesInfo
            if (seriesInfo != null) {
                episodeText.visibility = View.VISIBLE
                episodeText.text = "S${seriesInfo.season}:E${seriesInfo.episode}"
            } else {
                episodeText.visibility = View.GONE
            }
            
            video.released?.let { date ->
                releasedText.visibility = View.VISIBLE
                releasedText.text = dateFormat.format(date)
            } ?: run {
                releasedText.visibility = View.GONE
            }
            
            video.overview?.let { overview ->
                overviewText.visibility = View.VISIBLE
                overviewText.text = overview
            } ?: run {
                overviewText.visibility = View.GONE
            }
            
            video.thumbnail?.let { url ->
                thumbnailImage.visibility = View.VISIBLE
                thumbnailImage.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.placeholder_landscape)
                }
            } ?: run {
                thumbnailImage.visibility = View.GONE
            }
            
            itemView.isSelected = isSelected
            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                notifyItemChanged(selectedPosition)
                
                onVideoClick(video)
            }
        }
    }
}

class VideoDiffCallback : DiffUtil.ItemCallback<Video>() {
    override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
        return oldItem == newItem
    }
}

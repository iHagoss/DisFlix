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
import com.stremio.app.data.models.Addon
import com.stremio.app.data.models.Stream
import com.stremio.app.data.models.StreamType

class StreamAdapter(
    private val onStreamClick: (Addon, Stream) -> Unit
) : ListAdapter<Pair<Addon, Stream>, StreamAdapter.StreamViewHolder>(StreamDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stream, parent, false)
        return StreamViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        val (addon, stream) = getItem(position)
        holder.bind(addon, stream)
    }
    
    inner class StreamViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val addonIcon: ImageView = itemView.findViewById(R.id.addon_icon)
        private val addonName: TextView = itemView.findViewById(R.id.addon_name)
        private val streamName: TextView = itemView.findViewById(R.id.stream_name)
        private val streamDescription: TextView = itemView.findViewById(R.id.stream_description)
        private val streamType: TextView = itemView.findViewById(R.id.stream_type)
        
        fun bind(addon: Addon, stream: Stream) {
            addonName.text = addon.manifest.name
            
            addon.manifest.logo?.let { url ->
                addonIcon.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_addon)
                }
            } ?: run {
                addonIcon.setImageResource(R.drawable.ic_addon)
            }
            
            streamName.text = stream.name ?: getDefaultStreamName(stream)
            
            stream.description?.let { desc ->
                streamDescription.visibility = View.VISIBLE
                streamDescription.text = desc
            } ?: run {
                streamDescription.visibility = View.GONE
            }
            
            streamType.text = when (stream.streamType) {
                StreamType.URL -> "Direct"
                StreamType.YOUTUBE -> "YouTube"
                StreamType.TORRENT -> "Torrent"
                StreamType.EXTERNAL -> "External"
                StreamType.UNKNOWN -> "Unknown"
            }
            
            itemView.setOnClickListener {
                onStreamClick(addon, stream)
            }
        }
        
        private fun getDefaultStreamName(stream: Stream): String {
            return when (stream.streamType) {
                StreamType.URL -> "Direct Stream"
                StreamType.YOUTUBE -> "YouTube Video"
                StreamType.TORRENT -> "Torrent Stream"
                StreamType.EXTERNAL -> "External Link"
                StreamType.UNKNOWN -> "Unknown Stream"
            }
        }
    }
}

class StreamDiffCallback : DiffUtil.ItemCallback<Pair<Addon, Stream>>() {
    override fun areItemsTheSame(
        oldItem: Pair<Addon, Stream>,
        newItem: Pair<Addon, Stream>
    ): Boolean {
        return oldItem.second.url == newItem.second.url &&
                oldItem.second.infoHash == newItem.second.infoHash
    }
    
    override fun areContentsTheSame(
        oldItem: Pair<Addon, Stream>,
        newItem: Pair<Addon, Stream>
    ): Boolean {
        return oldItem == newItem
    }
}

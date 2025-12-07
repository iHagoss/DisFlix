package com.stremio.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.stremio.app.R
import com.stremio.app.data.models.MetaItemPreview

class MetaPreviewAdapter(
    private val onItemClick: (MetaItemPreview) -> Unit
) : ListAdapter<MetaItemPreview, MetaPreviewAdapter.MetaViewHolder>(MetaPreviewDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MetaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meta_preview, parent, false)
        return MetaViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: MetaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class MetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posterImage: ImageView = itemView.findViewById(R.id.poster_image)
        private val titleText: TextView = itemView.findViewById(R.id.title_text)
        
        fun bind(item: MetaItemPreview) {
            titleText.text = item.name
            
            item.poster?.let { url ->
                Picasso.get()
                    .load(url)
                    .placeholder(R.drawable.placeholder_poster)
                    .error(R.drawable.placeholder_poster)
                    .into(posterImage)
            } ?: run {
                posterImage.setImageResource(R.drawable.placeholder_poster)
            }
            
            itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

class MetaPreviewDiffCallback : DiffUtil.ItemCallback<MetaItemPreview>() {
    override fun areItemsTheSame(oldItem: MetaItemPreview, newItem: MetaItemPreview): Boolean {
        return oldItem.id == newItem.id
    }
    
    override fun areContentsTheSame(oldItem: MetaItemPreview, newItem: MetaItemPreview): Boolean {
        return oldItem == newItem
    }
}

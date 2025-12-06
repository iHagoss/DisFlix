package com.stremio.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.stremio.app.R
import com.stremio.app.data.models.DiscoverRow
import com.stremio.app.data.models.MetaItemPreview

class DiscoverAdapter(
    private val onItemClick: (MetaItemPreview) -> Unit
) : ListAdapter<DiscoverRow, DiscoverAdapter.RowViewHolder>(DiscoverRowDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_discover_row, parent, false)
        return RowViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.row_title)
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.row_recycler_view)
        
        fun bind(row: DiscoverRow) {
            titleText.text = row.title
            
            val adapter = MetaPreviewAdapter { item ->
                onItemClick(item)
            }
            
            recyclerView.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                this.adapter = adapter
            }
            
            adapter.submitList(row.items)
        }
    }
}

class DiscoverRowDiffCallback : DiffUtil.ItemCallback<DiscoverRow>() {
    override fun areItemsTheSame(oldItem: DiscoverRow, newItem: DiscoverRow): Boolean {
        return oldItem.deepLink == newItem.deepLink
    }
    
    override fun areContentsTheSame(oldItem: DiscoverRow, newItem: DiscoverRow): Boolean {
        return oldItem == newItem
    }
}

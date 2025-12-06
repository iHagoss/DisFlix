package com.stremio.app.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.stremio.app.R
import com.stremio.app.data.models.Addon

class AddonAdapter(
    private val onAddonClick: (Addon) -> Unit,
    private val onUninstallClick: (Addon) -> Unit
) : ListAdapter<Addon, AddonAdapter.AddonViewHolder>(AddonDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_addon, parent, false)
        return AddonViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: AddonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class AddonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val logoImage: ImageView = itemView.findViewById(R.id.addon_logo)
        private val nameText: TextView = itemView.findViewById(R.id.addon_name)
        private val descriptionText: TextView = itemView.findViewById(R.id.addon_description)
        private val typesText: TextView = itemView.findViewById(R.id.addon_types)
        private val officialBadge: TextView = itemView.findViewById(R.id.official_badge)
        private val uninstallButton: ImageButton = itemView.findViewById(R.id.uninstall_button)
        
        fun bind(addon: Addon) {
            val manifest = addon.manifest
            
            nameText.text = manifest.name
            descriptionText.text = manifest.description ?: "No description"
            typesText.text = manifest.types.joinToString(", ").uppercase()
            
            manifest.logo?.let { url ->
                logoImage.load(url) {
                    crossfade(true)
                    placeholder(R.drawable.ic_addon)
                    error(R.drawable.ic_addon)
                }
            } ?: run {
                logoImage.setImageResource(R.drawable.ic_addon)
            }
            
            if (addon.flags.official) {
                officialBadge.visibility = View.VISIBLE
                uninstallButton.visibility = View.GONE
            } else {
                officialBadge.visibility = View.GONE
                uninstallButton.visibility = View.VISIBLE
            }
            
            itemView.setOnClickListener {
                onAddonClick(addon)
            }
            
            uninstallButton.setOnClickListener {
                onUninstallClick(addon)
            }
        }
    }
}

class AddonDiffCallback : DiffUtil.ItemCallback<Addon>() {
    override fun areItemsTheSame(oldItem: Addon, newItem: Addon): Boolean {
        return oldItem.manifest.id == newItem.manifest.id
    }
    
    override fun areContentsTheSame(oldItem: Addon, newItem: Addon): Boolean {
        return oldItem == newItem
    }
}

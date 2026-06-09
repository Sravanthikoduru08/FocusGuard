package com.example.focusguard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focusguard.databinding.ItemAppSelectionBinding
import com.example.focusguard.engine.AppInfo

class AppSelectionAdapter(
    private val onAppToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>() {

    private var apps = listOf<AppInfo>()

    fun submitList(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged() // Simple notification, could use DiffUtil for better performance
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(private val binding: ItemAppSelectionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(appInfo: AppInfo) {
            binding.tvAppName.text = appInfo.appName
            binding.ivAppIcon.setImageDrawable(appInfo.icon)
            
            // Remove listener before setting checked state to avoid unwanted triggers during recycling
            binding.switchBlock.setOnCheckedChangeListener(null)
            binding.switchBlock.isChecked = appInfo.isBlocked
            
            binding.switchBlock.setOnCheckedChangeListener { _, isChecked ->
                appInfo.isBlocked = isChecked
                onAppToggled(appInfo, isChecked)
            }
        }
    }
}

package com.example.focusguard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.focusguard.databinding.ItemShopCardBinding

data class ShopItem(val id: String, val name: String, val description: String, val priceText: String, val cost: Int)

class ShopAdapter(
    private val items: List<ShopItem>,
    private val onPurchase: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemShopCardBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemShopCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvItemName.text = item.name
        holder.binding.tvItemDescription.text = item.description
        holder.binding.tvItemPrice.text = item.priceText
        
        holder.itemView.setOnClickListener {
            onPurchase(item)
        }
    }

    override fun getItemCount() = items.size
}

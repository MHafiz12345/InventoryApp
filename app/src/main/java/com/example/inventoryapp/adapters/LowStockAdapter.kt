package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemLowStockBinding
import com.example.inventoryapp.models.InventoryItem

class LowStockAdapter(
    private val onItemClick: ((InventoryItem) -> Unit)? = null
) : RecyclerView.Adapter<LowStockAdapter.LowStockViewHolder>() {

    private var items: List<InventoryItem> = emptyList()

    fun updateItems(newItems: List<InventoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LowStockViewHolder {
        val binding = ItemLowStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LowStockViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: LowStockViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class LowStockViewHolder(
        private val binding: ItemLowStockBinding,
        private val onItemClick: ((InventoryItem) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItem) {
            binding.apply {
                itemNameText.text = item.name
                locationText.text = buildString {
                    append(item.location.warehouseName)
                    append(" - ")
                    append(item.location.floor)
                    append(" - ")
                    append(item.location.section)
                }
                stockText.text = "${item.currentStock}/${item.minStock}"

                // Set click listener
                root.setOnClickListener {
                    onItemClick?.invoke(item)
                }
            }
        }
    }
}
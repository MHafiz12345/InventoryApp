package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.databinding.ItemLowStockBinding
import com.example.inventoryapp.models.LowStockItem

class LowStockAdapter : RecyclerView.Adapter<LowStockAdapter.LowStockViewHolder>() {
    private var items = listOf<LowStockItem>()
    var onItemClick: ((LowStockItem) -> Unit)? = null

    inner class LowStockViewHolder(private val binding: ItemLowStockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LowStockItem) {
            binding.apply {
                itemNameText.text = item.name
                stockText.text = "${item.currentStock}/${item.minStock}"
                locationText.text = "${item.warehouseName} - ${item.floor} ${item.location}"

                root.setOnClickListener {
                    onItemClick?.invoke(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LowStockViewHolder {
        val binding = ItemLowStockBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return LowStockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LowStockViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<LowStockItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
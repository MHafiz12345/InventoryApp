package com.example.inventoryapp.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemInventoryBinding
import com.example.inventoryapp.models.Category
import com.example.inventoryapp.models.InventoryItem
import com.example.inventoryapp.models.Warehouse
import java.text.NumberFormat
import java.util.Locale

class InventoryAdapter(
    private val warehouses: List<Warehouse>,
    private val categories: List<Category>
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private var items = listOf<InventoryItem>()
    var onItemClick: ((InventoryItem) -> Unit)? = null
    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

    inner class InventoryViewHolder(private val binding: ItemInventoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: InventoryItem) {
            binding.apply {
                // Basic item info
                itemName.text = item.name
                itemPrice.text = currencyFormatter.format(item.price)

                // Status chip
                statusChip.apply {
                    text = item.status
                    visibility = View.VISIBLE
                    val statusColor = when (item.status.lowercase()) {
                        "active" -> R.color.status_active
                        "inactive" -> R.color.status_inactive
                        "discontinued" -> R.color.status_discontinued
                        else -> R.color.status_active
                    }
                    backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(context, statusColor)
                    )
                }

                // Category chip
                val category = categories.find { it.id == item.categoryId }
                categoryChip.apply {
                    text = category?.name ?: "Unknown Category"
                    visibility = View.VISIBLE
                }

                // SKU
                skuText.text = item.sku

                // Warehouse and location
                val warehouse = warehouses.find { it.id == item.warehouseId }
                warehouseLocation.text = warehouse?.name ?: "Unknown Location"

                // Floor and Section
                val locationDetails = buildString {
                    if (!item.floor.isNullOrBlank()) append("Floor ${item.floor}")
                    if (!item.section.isNullOrBlank()) {
                        if (length > 0) append(" • ")
                        append("Section ${item.section}")
                    }
                }
                floorAndSection.text = locationDetails
                floorAndSection.visibility = if (locationDetails.isBlank()) View.GONE else View.VISIBLE

                // Stock information
                val isLowStock = item.currentStock <= item.minStock
                stockCount.text = "${item.currentStock} units"

                // Show min stock warning if needed
                minStockIndicator.apply {
                    visibility = if (isLowStock) View.VISIBLE else View.GONE
                    text = if (isLowStock) "Below minimum (${item.minStock})" else ""
                }

                // Stock count styling based on level
                stockCount.apply {
                    val stockColor = when {
                        isLowStock -> R.color.warning
                        else -> R.color.dark_gray
                    }
                    setTextColor(ContextCompat.getColor(context, stockColor))

                    // Update stock icon tint
                    val stockDrawable = ContextCompat.getDrawable(context, R.drawable.ic_stock)?.mutate()
                    stockDrawable?.setTint(ContextCompat.getColor(context, stockColor))
                    setCompoundDrawablesWithIntrinsicBounds(stockDrawable, null, null, null)
                }

                // Warehouse icon tint
                val warehouseDrawable = warehouseLocation.compoundDrawables[0]?.mutate()
                warehouseDrawable?.setTint(ContextCompat.getColor(root.context, R.color.dark_gray))
                warehouseLocation.setCompoundDrawablesWithIntrinsicBounds(
                    warehouseDrawable, null, null, null
                )

                // Click listener
                root.setOnClickListener {
                    onItemClick?.invoke(item)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val binding = ItemInventoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return InventoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    fun updateItems(newItems: List<InventoryItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
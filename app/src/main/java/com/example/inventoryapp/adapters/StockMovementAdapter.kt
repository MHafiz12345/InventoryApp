package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemStockMovementBinding
import com.example.inventoryapp.models.StockMovement
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class StockMovementAdapter : ListAdapter<StockMovement, StockMovementAdapter.ViewHolder>(StockMovementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStockMovementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemStockMovementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(movement: StockMovement) {
            binding.apply {
                // Set movement text
                val quantityText = abs(movement.quantity).toString()
                movementText.text = when {
                    movement.quantity > 0 -> "+$quantityText added"
                    movement.quantity < 0 -> "-$quantityText removed"
                    else -> "No change"
                }

                // Set reason if available
                movementReason.text = movement.reason ?: "No reason provided"

                // Set icon and background color
                val iconRes = when {
                    movement.quantity > 0 -> R.drawable.ic_arrow_up
                    movement.quantity < 0 -> R.drawable.ic_arrow_down
                    else -> R.drawable.ic_arrow_right
                }
                movementIcon.setImageResource(iconRes)

                val backgroundColor = when {
                    movement.quantity > 0 -> R.color.status_active
                    movement.quantity < 0 -> R.color.status_discontinued
                    else -> R.color.status_inactive
                }
                movementIcon.background.setTint(
                    ContextCompat.getColor(root.context, backgroundColor)
                )

                // Format timestamp
                val instant = Instant.parse(movement.timestamp)
                val localDateTime = instant.atZone(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                timestampText.text = formatter.format(localDateTime)
            }
        }
    }

    class StockMovementDiffCallback : DiffUtil.ItemCallback<StockMovement>() {
        override fun areItemsTheSame(oldItem: StockMovement, newItem: StockMovement): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StockMovement, newItem: StockMovement): Boolean {
            return oldItem == newItem
        }
    }
}
package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemActivityLogBinding
import com.example.inventoryapp.models.ActivityLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ActivityLogAdapter : ListAdapter<ActivityLog, ActivityLogAdapter.ViewHolder>(ActivityLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActivityLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemActivityLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(log: ActivityLog) {
            binding.apply {
                // Set action text based on type
                activityText.text = when (log.actionType) {
                    "CREATE" -> "Item created"
                    "UPDATE" -> "Item updated"
                    "DELETE" -> "Item deleted"
                    "STOCK_UPDATE" -> "Stock adjusted"
                    else -> log.actionType
                }

                // Set icon based on action
                val iconRes = when (log.actionType) {
                    "CREATE" -> R.drawable.ic_add
                    "UPDATE" -> R.drawable.ic_edit
                    "DELETE" -> R.drawable.ic_delete
                    "STOCK_UPDATE" -> R.drawable.ic_stock
                    else -> R.drawable.ic_info
                }
                activityIcon.setImageResource(iconRes)

                // Set details if available
                if (!log.details.isNullOrBlank()) {
                    activityDetails.text = log.details
                    activityDetails.visibility = View.VISIBLE
                } else {
                    activityDetails.visibility = View.GONE
                }

                // Set user
                activityUser.text = "By: ${log.userId}"

                // Format timestamp
                val instant = Instant.parse(log.timestamp)
                val localDateTime = instant.atZone(ZoneId.systemDefault())
                val formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm")
                timestampText.text = formatter.format(localDateTime)

                // Set icon background color
                val backgroundColor = when (log.actionType) {
                    "CREATE" -> R.color.status_active
                    "DELETE" -> R.color.status_discontinued
                    else -> R.color.dark_blue
                }
                activityIcon.background.setTint(
                    ContextCompat.getColor(root.context, backgroundColor)
                )
            }
        }
    }

    class ActivityLogDiffCallback : DiffUtil.ItemCallback<ActivityLog>() {
        override fun areItemsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ActivityLog, newItem: ActivityLog): Boolean {
            return oldItem == newItem
        }
    }
}
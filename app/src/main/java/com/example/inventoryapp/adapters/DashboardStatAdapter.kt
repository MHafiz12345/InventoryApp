package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemDashboardStatBinding
import com.example.inventoryapp.models.DashboardStat

class DashboardStatAdapter : RecyclerView.Adapter<DashboardStatAdapter.StatViewHolder>() {
    private var stats: List<DashboardStat> = emptyList()

    fun updateStats(newStats: List<DashboardStat>) {
        stats = newStats
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = ItemDashboardStatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(stats[position])
    }

    override fun getItemCount(): Int = stats.size

    class StatViewHolder(private val binding: ItemDashboardStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stat: DashboardStat) {
            with(binding) {
                // Set icon
                statIcon.setImageResource(stat.icon)

                // Set icon tint based on warning state
                statIcon.setColorFilter(
                    ContextCompat.getColor(
                        root.context,
                        if (stat.warning) R.color.warning else R.color.primary
                    )
                )

                // Set title and value
                statTitle.text = stat.title
                statValue.text = stat.value

                // Set value text color for warnings
                if (stat.warning) {
                    statValue.setTextColor(
                        ContextCompat.getColor(root.context, R.color.warning)
                    )
                } else {
                    statValue.setTextColor(
                        ContextCompat.getColor(root.context, R.color.text_primary)
                    )
                }
            }
        }
    }
}
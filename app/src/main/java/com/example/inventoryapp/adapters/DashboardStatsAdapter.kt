package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemDashboardStatBinding
import com.example.inventoryapp.models.DashboardStat
import com.google.android.material.card.MaterialCardView

class DashboardStatsAdapter : RecyclerView.Adapter<DashboardStatsAdapter.StatViewHolder>() {
    private var stats = listOf<DashboardStat>()

    inner class StatViewHolder(private val binding: ItemDashboardStatBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(stat: DashboardStat) {
            binding.apply {
                statIcon.setImageResource(stat.icon)
                statTitle.text = stat.title
                statValue.text = stat.value

                if (stat.warning) {
                    (root as MaterialCardView).setCardBackgroundColor(
                        root.context.getColor(R.color.warning)
                    )
                    statValue.setTextColor(root.context.getColor(android.R.color.white))
                    statTitle.setTextColor(root.context.getColor(android.R.color.white))
                    statIcon.setColorFilter(root.context.getColor(android.R.color.white))
                } else {
                    (root as MaterialCardView).setCardBackgroundColor(
                        root.context.getColor(android.R.color.white)
                    )
                    statValue.setTextColor(root.context.getColor(R.color.text_primary))
                    statTitle.setTextColor(root.context.getColor(R.color.text_secondary))
                    statIcon.setColorFilter(root.context.getColor(R.color.primary))
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatViewHolder {
        val binding = ItemDashboardStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StatViewHolder, position: Int) {
        holder.bind(stats[position])
    }

    override fun getItemCount() = stats.size

    fun updateStats(newStats: List<DashboardStat>) {
        stats = newStats
        notifyDataSetChanged()
    }
}
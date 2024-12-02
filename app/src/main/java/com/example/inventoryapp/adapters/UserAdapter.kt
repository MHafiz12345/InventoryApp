package com.example.inventoryapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ItemUserBinding
import com.example.inventoryapp.models.UserData
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class UserAdapter(
    private val onUserClick: (UserData) -> Unit,
    private val onActiveToggle: (UserData, Boolean) -> Unit
) : ListAdapter<UserData, UserAdapter.ViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class ViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserData, position: Int) {
            binding.apply {
                userEmail.text = user.email

                // Role chip
                roleChip.text = user.role.capitalize()
                roleChip.setBackgroundResource(
                    if (user.role == "admin") R.drawable.bg_admin_chip
                    else R.drawable.bg_staff_chip
                )

                // Active status
                activeSwitch.isChecked = user.isActive

                // Last login
                val instant = Instant.parse(user.lastLogin)
                val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                    .withZone(ZoneId.systemDefault())
                lastLoginText.text = "Last login: ${formatter.format(instant)}"

                // Click listeners with position
                root.setOnClickListener { onUserClick(user) }
                activeSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onActiveToggle(user, isChecked)
                }
            }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<UserData>() {
        override fun areItemsTheSame(oldItem: UserData, newItem: UserData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: UserData, newItem: UserData): Boolean {
            return oldItem == newItem
        }
    }
}

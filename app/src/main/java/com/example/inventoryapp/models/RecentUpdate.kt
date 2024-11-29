package com.example.inventoryapp.models

data class RecentUpdate(
    val itemId: String,
    val itemName: String,
    val action: String, // "Added", "Removed", "Updated"
    val quantity: Int,
    val timestamp: Long,
    val updatedBy: String
)
package com.example.inventoryapp.models

data class InventoryItem(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val currentStock: Int = 0,
    val minStock: Int = 0,
    val price: Double = 0.0,
    val location: ItemLocation = ItemLocation(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val updatedBy: String = ""
)
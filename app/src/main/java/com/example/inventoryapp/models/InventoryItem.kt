package com.example.inventoryapp.models

data class InventoryItem(
    val id: String,
    val name: String,
    val description: String?,
    val categoryId: String,
    var currentStock: Int,
    val minStock: Int,
    val price: Float,
    val warehouseId: String,
    val floor: String?,
    val section: String?,
    val status: String,
    val sku: String
)

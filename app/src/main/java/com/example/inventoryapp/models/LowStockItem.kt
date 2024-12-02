package com.example.inventoryapp.models

data class LowStockItem(
    val id: String = "",
    val name: String = "",
    val currentStock: Int = 0,
    val minStock: Int = 0,
    val warehouseName: String = "",
    val floor: String = "",
    val location: String = "",
    val sku: String = "" // Added the missing 'sku' field
)

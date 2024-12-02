package com.example.inventoryapp.models

data class StockLevel(
    val id: String,
    val itemId: String,
    val warehouseId: String,
    val quantity: Int,
    val location: String? = null,
    val lastUpdated: String
)
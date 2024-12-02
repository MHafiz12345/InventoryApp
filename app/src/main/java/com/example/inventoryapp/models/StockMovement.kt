package com.example.inventoryapp.models

data class StockMovement(
    val id: String,
    val itemId: String,
    val warehouseId: String,
    val quantity: Int,
    val type: String,
    val reason: String?,
    val userId: String,
    val timestamp: String // Add timestamp property
)
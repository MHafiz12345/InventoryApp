package com.example.inventoryapp.models

data class DashboardStats(
    val totalItems: Int = 0,
    val totalValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val warehouseCount: Int = 0,
    val monthlyTrends: Map<String, Int> = emptyMap(),
    val categoryDistribution: Map<String, Int> = emptyMap()
)
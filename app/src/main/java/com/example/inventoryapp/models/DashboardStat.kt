package com.example.inventoryapp.models

data class DashboardStat(
    val title: String,
    val value: String,
    val icon: Int, // Resource ID for the icon
    val warning: Boolean = false
)
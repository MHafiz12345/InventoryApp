package com.example.inventoryapp.models

data class ActivityLog(
    val id: String,
    val userId: String,
    val itemId: String,
    val actionType: String,
    val oldValue: String?,
    val newValue: String?,
    val details: String?,
    val timestamp: String // Add timestamp property
)
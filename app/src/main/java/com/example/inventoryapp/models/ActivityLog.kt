package com.example.inventoryapp.models

data class ActivityLog(
    val id: String = "",
    val type: ActivityType = ActivityType.UPDATE,
    val itemId: String = "",
    val userId: String = "",
    val timestamp: Long = 0,
    val details: ActivityDetails = ActivityDetails()
)
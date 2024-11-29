package com.example.inventoryapp.models

data class UserData(
    val uid: String = "",
    val email: String = "",
    val role: String = "staff", // Default role
    val department: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = 0,
    val lastLogin: Long = 0,
    val permissions: List<String> = emptyList()
)

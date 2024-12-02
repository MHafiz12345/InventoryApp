package com.example.inventoryapp.models

data class UserData(
    val id: String,               // Appwrite document ID
    val email: String,            // User email
    val role: String,            // "admin" or "staff"
    val isActive: Boolean,       // Account status
    val lastLogin: String        // Using Appwrite's $createdAt/timestamp
) {
    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_STAFF = "staff"
    }
}
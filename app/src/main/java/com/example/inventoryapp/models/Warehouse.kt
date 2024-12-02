package com.example.inventoryapp.models

data class Warehouse(
    val id: String,
    val name: String,
    val location: String,
    val address: String?,
    val floors: List<String>,    // Changed to List since we store arrays
    val sections: List<String>   // Changed to List since we store arrays
)
package com.example.inventoryapp.models

data class Warehouse(
    var id: String = "",
    val name: String = "",
    val floors: Map<String, Floor> = emptyMap()
)
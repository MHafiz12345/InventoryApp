package com.example.inventoryapp.models

data class Floor(
    val name: String = "",
    val sections: Map<String, Section> = emptyMap()
)
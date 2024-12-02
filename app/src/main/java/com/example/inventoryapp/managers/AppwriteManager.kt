package com.example.inventoryapp

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Databases
import io.appwrite.services.Storage

object AppwriteManager {
    private lateinit var client: Client

    // Constants
    const val ENDPOINT = "https://cloud.appwrite.io/v1"
    const val PROJECT_ID = "674c0305003e408c4878"
    const val DATABASE_ID = "inventory_management"

    // Initialize client
    fun initialize(context: Context) {
        try {
            client = Client()  // Remove context parameter
                .setEndpoint(ENDPOINT)
                .setProject(PROJECT_ID)
                .setSelfSigned(false)  // Set to true only if using self-signed certificates

            println("Appwrite Client initialized with Project ID: $PROJECT_ID")
        } catch (e: Exception) {
            println("Failed to initialize Appwrite client: ${e.message}")
            throw e
        }
    }

    // Rest of your code remains the same
    fun getClient(): Client = client

    val databases by lazy {
        Databases(client)
    }

    val storage by lazy {
        Storage(client)
    }

    object Collections {
        const val WAREHOUSES = "warehouses"
        const val CATEGORIES = "categories"
        const val INVENTORY_ITEMS = "inventory_items"
        const val STOCK_LEVELS = "stock_levels"
        const val ACTIVITY_LOGS = "activity_logs"
        const val STOCK_MOVEMENTS = "stock_movements"
        const val USERS = "users"
    }
}
/*package com.example.inventoryapp.setup

import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Databases
import io.appwrite.enums.IndexType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

object AppwriteDatabaseSetup {
    private val client = Client()
        .setEndpoint("https://cloud.appwrite.io/v1")
        .setProject("674c0305003e408c4878")
        .setKey("standard_fcc0e33266bae103f20a8487753a4afa6c34639410723714be08d8cc3b715989bc160b4bd8d5295d2c6b16518649a05f499ac343e2e15b033d54aef46904caa561e3c43e29ddac7ca7951b8ba632679ab32882fe23790c9c2ff390053f5b3fd827f9c2938de76c4a9ad3f3d8003b2ea43c0c0b5f1b8e1f702c75ed510e3345da")

    private val databases = Databases(client)
    private const val DATABASE_ID = "inventory_management"


    public suspend fun createInitialData() {
        try {
            // Create initial warehouse
            val warehouseData = mapOf(
                "name" to "Main Warehouse",
                "location" to "Main Street",
                "address" to "123 Main St",
                "floors" to listOf("Ground", "First", "Second"),
                "sections" to listOf("A", "B", "C", "D")
            )

            databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = "warehouses",
                documentId = ID.unique(),
                data = warehouseData
            )

            // Create initial category
            val categoryData = mapOf(
                "name" to "General",
                "description" to "General items"
            )

            databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = "categories",
                documentId = ID.unique(),
                data = categoryData
            )

            // Create admin user
            val adminData = mapOf(
                "email" to "mhemail12345@gmail.com",
                "role" to "admin",
                "isActive" to true,
                "lastLogin" to Instant.now().toString()
            )

            databases.createDocument(
                databaseId = DATABASE_ID,
                collectionId = "users",
                documentId = ID.unique(),
                data = adminData
            )

        } catch (e: AppwriteException) {
            Log.e("Setup", "Error creating initial data: ${e.message}")
        }
    }
}*/
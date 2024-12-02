package com.example.inventoryapp.managers

import com.example.inventoryapp.AppwriteManager
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account

object RoleManager {
    private val account = Account(AppwriteManager.getClient())

    suspend fun isAdmin(): Boolean {
        return try {
            val user = account.get()
            val doc = AppwriteManager.databases.getDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.USERS,
                documentId = user.id
            )
            (doc.data["role"] as? String) == "admin"
        } catch (e: AppwriteException) {
            false
        }
    }

    suspend fun getCurrentUserRole(): String {
        return try {
            val user = account.get()
            val doc = AppwriteManager.databases.getDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.USERS,
                documentId = user.id
            )
            (doc.data["role"] as? String) ?: "staff"
        } catch (e: AppwriteException) {
            "staff"
        }
    }

    // Example usage in activities:
    // Navigate based on role
    suspend fun shouldAllowUserManagement(): Boolean = isAdmin()

    // Check specific permissions
    suspend fun canEditInventory(): Boolean = true  // Both admin and staff can edit
    suspend fun canViewReports(): Boolean = isAdmin()  // Only admin can view reports
    suspend fun canManageUsers(): Boolean = isAdmin()  // Only admin can manage users
}
package com.example.inventoryapp.utils

object Constants {
    // Firebase Database References
    const val REF_USERS = "users"
    const val REF_WAREHOUSES = "warehouses"
    const val REF_INVENTORY = "inventory"
    const val REF_CATEGORIES = "categories"
    const val REF_ACTIVITY_LOGS = "activity_logs"

    // Appwrite
    const val APPWRITE_ENDPOINT = "https://cloud.appwrite.io/v1"
    const val APPWRITE_PROJECT_ID = "6746d4d50001acd552be"
    const val APPWRITE_BUCKET_ID = "6746dc160025694a997e"

    // Shared Preferences
    const val PREF_NAME = "warewise_prefs"
    const val PREF_USER_ID = "user_id"
    const val PREF_USER_ROLE = "user_role"
}
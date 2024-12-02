package com.example.inventoryapp

import android.app.Application

class InventoryApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppwriteManager.initialize(this)
    }
}
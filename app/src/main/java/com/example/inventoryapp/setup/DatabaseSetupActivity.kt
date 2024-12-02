/*package com.example.inventoryapp.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.activities.DashboardActivity
import kotlinx.coroutines.launch

class DatabaseSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Feedback during database setup process
        lifecycleScope.launch {
            try {
                showToast("Setting up database...")

                // Step 1: Initialize the Warehouses collection and add sample data
                AppwriteDatabaseSetup.createInitialData()

                // Show success message
                showToast("Setup complete!")

                // Navigate to the dashboard
                navigateToDashboard()
            } catch (e: Exception) {
                // Show error message with details
                showToast("Setup failed: ${e.message}")
                e.printStackTrace() // Log the stack trace for debugging
            }
        }
    }

    /**
     * Utility method to show a toast message.
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Navigate to the dashboard activity.
     */
    private fun navigateToDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
    }*/
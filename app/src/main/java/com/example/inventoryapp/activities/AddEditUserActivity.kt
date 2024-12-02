package com.example.inventoryapp.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.databinding.ActivityAddEditUserBinding
import com.example.inventoryapp.models.UserData
import io.appwrite.ID
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.launch
import java.time.Instant

class AddEditUserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditUserBinding
    private var userId: String? = null
    private var currentUser: UserData? = null
    private val account = Account(AppwriteManager.getClient())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId")
        setupToolbar()
        setupViews()

        if (userId != null) {
            loadUser()
        } else {
            binding.passwordLayout.helperText = "Required for new users"
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.toolbarTitle.text = if (userId != null) "Edit User" else "Add User"
    }

    private fun setupViews() {
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                if (userId != null) {
                    updateUser()
                } else {
                    createUser()
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email address"
            return false
        }

        if (userId == null && password.isEmpty()) {
            binding.passwordLayout.error = "Password is required for new users"
            return false
        }

        if (password.isNotEmpty() && password.length < 8) {
            binding.passwordLayout.error = "Password must be at least 8 characters long"
            return false
        }

        return true
    }

    private fun getUserData(): Map<String, Any> {
        return mapOf(
            "email" to binding.emailInput.text.toString().trim(),
            "role" to if (binding.adminRole.isChecked) UserData.ROLE_ADMIN else UserData.ROLE_STAFF,
            "isActive" to binding.activeSwitch.isChecked
        )
    }

    private fun loadUser() {
        lifecycleScope.launch {
            try {
                val doc = AppwriteManager.databases.getDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    documentId = userId!!
                )

                currentUser = UserData(
                    id = doc.id,
                    email = doc.data["email"] as String,
                    role = doc.data["role"] as String,
                    isActive = doc.data["isActive"] as Boolean,
                    lastLogin = doc.data["\$createdAt"] as String
                )

                // Populate fields
                binding.apply {
                    emailInput.setText(currentUser?.email)
                    if (currentUser?.role == UserData.ROLE_ADMIN) {
                        adminRole.isChecked = true
                    } else {
                        staffRole.isChecked = true
                    }
                    activeSwitch.isChecked = currentUser?.isActive ?: true
                    passwordLayout.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                Toast.makeText(this@AddEditUserActivity,
                    "Failed to load user: ${e.message}",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun createUser() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString()

        lifecycleScope.launch {
            try {
                // First create the account
                val user = account.create(
                    userId = ID.unique(),
                    email = email,
                    password = password
                )

                // Then create the user document with additional info
                val userData = getUserData().toMutableMap()
                userData["lastLogin"] = Instant.now().toString()

                AppwriteManager.databases.createDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    documentId = user.id,
                    data = userData
                )

                Toast.makeText(this@AddEditUserActivity,
                    "User created successfully",
                    Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: AppwriteException) {
                Toast.makeText(this@AddEditUserActivity,
                    "Failed to create user: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUser() {
        lifecycleScope.launch {
            try {
                // Update user document
                AppwriteManager.databases.updateDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    documentId = userId!!,
                    data = getUserData()
                )

                // Update password if provided
                val password = binding.passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    account.updatePassword(password)
                }

                Toast.makeText(this@AddEditUserActivity,
                    "User updated successfully",
                    Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: AppwriteException) {
                Toast.makeText(this@AddEditUserActivity,
                    "Failed to update user: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
}
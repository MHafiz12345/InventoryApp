package com.example.inventoryapp.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.databinding.ActivityLogInBinding
import com.example.inventoryapp.models.UserData
import com.example.inventoryapp.utils.LoadingDialog
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.launch
import java.time.Instant

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLogInBinding
    private lateinit var loadingDialog: LoadingDialog
    private val account = Account(AppwriteManager.getClient())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = LoadingDialog(this)

        setupViews()
        // Launch checkSession in a coroutine
        lifecycleScope.launch {
            checkSession()
        }
        setupRememberMe()
    }

    private fun setupViews() {
        binding.apply {
            loginButton.setOnClickListener {
                if (validateInputs()) {
                    loginUser()
                }
            }

            forgotPasswordText.setOnClickListener {
                // Create intent to launch ForgotPasswordActivity
                val intent = Intent(this@LoginActivity, ForgotPasswordActivity::class.java)

                // Optionally pass the email if it's already entered
                val email = binding.emailInput.text.toString().trim()
                if (email.isNotEmpty()) {
                    intent.putExtra("email", email)
                }

                // Start the activity
                startActivity(intent)
            }
        }
    }

    private fun setupRememberMe() {
        val sharedPref = getSharedPreferences("inventory_app_pref", Context.MODE_PRIVATE)
        binding.rememberMeCheckbox.isChecked = sharedPref.getBoolean("remember_me", false)

        if (binding.rememberMeCheckbox.isChecked) {
            binding.emailInput.setText(sharedPref.getString("email", ""))
        }
    }

    private fun saveEmailToPreferences(email: String) {
        val sharedPref = getSharedPreferences("inventory_app_pref", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("remember_me", binding.rememberMeCheckbox.isChecked)
            putString("email", if (binding.rememberMeCheckbox.isChecked) email else "")
            apply()
        }
    }

    private fun validateInputs(): Boolean {
        clearErrors()
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()
        var isValid = true

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty() || password.length < 8) {
            binding.passwordInputLayout.error = "Password must be at least 8 characters"
            isValid = false
        }

        return isValid
    }

    private fun loginUser() {
        val email = binding.emailInput.text.toString().trim()
        val password = binding.passwordInput.text.toString().trim()

        showLoading(true)
        lifecycleScope.launch {
            try {
                // Create email session with correct method name
                val session = account.createEmailPasswordSession(email, password)

                // Get user info
                val user = account.get()

                // Check user in database
                checkUserInDatabase(user.id, email)
            } catch (e: AppwriteException) {
                handleFailedLogin(e.message)
                showLoading(false)
            }
        }
    }

    private suspend fun checkUserInDatabase(userId: String, email: String) {
        try {
            val doc = AppwriteManager.databases.getDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.USERS,
                documentId = userId
            )

            val userData = UserData(
                id = doc.id,
                email = doc.data["email"] as String,
                role = doc.data["role"] as String,
                isActive = doc.data["isActive"] as Boolean,
                lastLogin = doc.data["\$createdAt"] as String
            )

            if (!userData.isActive) {
                showError("Account is inactive. Contact admin.")
                account.deleteSession("current")  // Logout if inactive
                showLoading(false)
                return
            }

            // Update last login
            AppwriteManager.databases.updateDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.USERS,
                documentId = userId,
                data = mapOf("lastLogin" to Instant.now().toString())
            )

            handleSuccessfulLogin(email)
        } catch (e: AppwriteException) {
            if (e.code == 404) {
                // User doesn't exist in database, create new user
                createUserInDatabase(userId, email)
            } else {
                showError("Database error: ${e.message}")
                showLoading(false)
            }
        }
    }

    private suspend fun createUserInDatabase(userId: String, email: String) {
        try {
            AppwriteManager.databases.createDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.USERS,
                documentId = userId,
                data = mapOf(
                    "email" to email,
                    "role" to UserData.ROLE_STAFF,
                    "isActive" to true,
                    "lastLogin" to Instant.now().toString()
                )
            )
            handleSuccessfulLogin(email)
        } catch (e: AppwriteException) {
            showError("Failed to create user profile: ${e.message}")
            showLoading(false)
        }
    }

    private fun handleSuccessfulLogin(email: String) {
        saveEmailToPreferences(email)
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun handleFailedLogin(errorMessage: String?) {
        showLoading(false)
        when {
            errorMessage?.contains("password") == true -> {
                binding.passwordInputLayout.error = "Incorrect password"
                showError("Login failed: Incorrect password - $errorMessage")
            }
            errorMessage?.contains("user") == true -> {
                binding.emailInputLayout.error = "No account found with this email"
                showError("Login failed: User not found - $errorMessage")
            }
            else -> {
                showError("Login failed: $errorMessage")
                println("Debug - Full error: $errorMessage")
            }
        }
    }

    private fun sendPasswordReset(email: String) {
        lifecycleScope.launch {
            try {
                account.createRecovery(
                    email = email,
                    url = "myapp://auth/reset-password"  // You'll need to handle this URL in your app
                )
                showSuccess("Password reset email sent")
            } catch (e: AppwriteException) {
                showError("Failed to send reset email: ${e.message}")
            }
        }
    }

    private suspend fun checkSession() {
        try {
            val session = account.getSession("current")
            if (session != null) {
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
        } catch (e: AppwriteException) {
            // No active session, user needs to log in
        }
    }

    private fun clearErrors() {
        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
    }

    private fun showLoading(show: Boolean) {
        if (show) loadingDialog.show() else loadingDialog.dismiss()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
package com.example.inventoryapp.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.databinding.ActivityForgotPasswordBinding
import com.example.inventoryapp.utils.LoadingDialog
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding
    private lateinit var loadingDialog: LoadingDialog
    private val account = Account(AppwriteManager.getClient())

    private var isCooldownActive = false
    private val cooldownDuration: Long = 30000 // 30 seconds cooldown

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadingDialog = LoadingDialog(this)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.resetButton.setOnClickListener {
            if (isCooldownActive) {
                Toast.makeText(this, "Please wait before trying again.", Toast.LENGTH_SHORT).show()
            } else {
                handlePasswordReset()
            }
        }

        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun handlePasswordReset() {
        val email = binding.emailInput.text.toString().trim()
        if (!validateEmail(email)) return

        loadingDialog.show()
        lifecycleScope.launch {
            try {
                // Check if user exists in database
                val users = AppwriteManager.databases.listDocuments(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    queries = listOf(io.appwrite.Query.equal("email", email))
                )

                if (users.documents.isEmpty()) {
                    showError("No account found with this email address")
                    loadingDialog.dismiss()
                    return@launch
                }

                // Send password reset email
                account.createRecovery(
                    email = email,
                    url = AppwriteManager.ENDPOINT
                )

                loadingDialog.dismiss()
                showSuccess("Password reset link has been sent to your email")
                startCooldown()
            } catch (e: AppwriteException) {
                loadingDialog.dismiss()
                showError("Failed to send reset email: ${e.message}")
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            binding.emailInputLayout.error = "Email is required"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = "Enter a valid email"
            false
        } else {
            binding.emailInputLayout.error = null
            true
        }
    }

    private fun startCooldown() {
        binding.resetButton.isEnabled = false
        object : CountDownTimer(cooldownDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isCooldownActive = true
                val secondsLeft = millisUntilFinished / 1000
                binding.resetButton.text = "Wait ${secondsLeft}s"
            }

            override fun onFinish() {
                isCooldownActive = false
                binding.resetButton.isEnabled = true
                binding.resetButton.text = "Send Reset Link"
            }
        }.start()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
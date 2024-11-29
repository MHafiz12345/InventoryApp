package com.example.inventoryapp.activities

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.inventoryapp.R
import com.example.inventoryapp.utils.LoadingDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    private lateinit var emailInput: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var resetButton: MaterialButton
    private lateinit var backButton: ImageView
    private lateinit var loadingDialog: LoadingDialog

    private var isCooldownActive = false
    private val cooldownDuration: Long = 30000 // 30 seconds cooldown

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        initializeFirebase()
        initializeViews()
        setupClickListeners()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()
    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        resetButton = findViewById(R.id.resetButton)
        backButton = findViewById(R.id.backButton)
        loadingDialog = LoadingDialog(this)
    }

    private fun setupClickListeners() {
        resetButton.setOnClickListener {
            if (isCooldownActive) {
                Toast.makeText(this, "Please wait before trying again.", Toast.LENGTH_SHORT).show()
            } else {
                handlePasswordReset()
            }
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun handlePasswordReset() {
        val email = emailInput.text.toString().trim()
        if (!validateEmail(email)) return

        loadingDialog.show()

        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    showSuccess("Password reset email sent to $email")
                    startCooldown()
                } else {
                    showError("Failed to send reset email. Please try again.")
                }
            }
    }

    private fun validateEmail(email: String): Boolean {
        return if (email.isEmpty()) {
            emailInputLayout.error = "Email is required"
            false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Enter a valid email"
            false
        } else {
            emailInputLayout.error = null
            true
        }
    }

    private fun startCooldown() {
        resetButton.isEnabled = false
        object : CountDownTimer(cooldownDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                isCooldownActive = true
                val secondsLeft = millisUntilFinished / 1000
                resetButton.text = "Wait ${secondsLeft}s"
            }

            override fun onFinish() {
                isCooldownActive = false
                resetButton.isEnabled = true
                resetButton.text = "Send Reset Link"
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

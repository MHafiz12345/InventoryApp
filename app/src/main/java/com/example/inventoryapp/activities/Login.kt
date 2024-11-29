package com.example.inventoryapp.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.inventoryapp.R
import com.example.inventoryapp.models.UserData
import com.example.inventoryapp.utils.LoadingDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LogIn : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference

    private lateinit var emailInput: TextInputEditText
    private lateinit var passwordInput: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var rememberMeCheckbox: CheckBox
    private lateinit var forgotPasswordText: TextView
    private lateinit var loadingDialog: LoadingDialog

    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)
        loadingDialog = LoadingDialog(this)

        initializeFirebase()
        initializeViews()
        setupClickListeners()
        checkUserSession()
        setupRememberMe()
    }

    private fun initializeFirebase() {
        auth = FirebaseAuth.getInstance()

        val dbUrl = "https://warewise-618a6-default-rtdb.asia-southeast1.firebasedatabase.app"
        database = FirebaseDatabase.getInstance(dbUrl)
        database.setPersistenceEnabled(true)
        usersRef = database.getReference("users")
        usersRef.keepSynced(true)

        auth.signOut()


    }

    private fun initializeViews() {
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        loginButton = findViewById(R.id.loginButton)
        rememberMeCheckbox = findViewById(R.id.rememberMeCheckbox)
        forgotPasswordText = findViewById(R.id.forgotPasswordText)
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateInputs()) {
                loginUser()
            }
        }

        forgotPasswordText.setOnClickListener {
            // Redirect to ForgotPasswordActivity
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupRememberMe() {
        val sharedPref = getSharedPreferences("inventory_app_pref", Context.MODE_PRIVATE)
        rememberMeCheckbox.isChecked = sharedPref.getBoolean("remember_me", false)

        if (rememberMeCheckbox.isChecked) {
            emailInput.setText(sharedPref.getString("email", ""))
        }
    }

    private fun saveEmailToPreferences(email: String) {
        val sharedPref = getSharedPreferences("inventory_app_pref", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("remember_me", rememberMeCheckbox.isChecked)
            putString("email", if (rememberMeCheckbox.isChecked) email else "")
            apply()
        }
    }

    private fun validateInputs(): Boolean {
        clearErrors()
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()
        var isValid = true

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Please enter a valid email"
            isValid = false
        }

        if (password.isEmpty() || password.length < 6) {
            passwordInputLayout.error = "Password must be at least 6 characters"
            isValid = false
        }

        return isValid
    }

    private fun loginUser() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        showLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val user = authResult.user
                if (user != null) {
                    checkUserInDatabase(user.uid, email)
                } else {
                    showError("Authentication error")
                    showLoading(false)
                }
            }
            .addOnFailureListener { exception ->
                showError("Authentication failed: ${exception.message}")
                handleFailedLogin(exception.message)
                showLoading(false)
            }
    }

    private fun checkUserInDatabase(uid: String, email: String) {
        usersRef.child(uid).get()
            .addOnSuccessListener { snapshot ->
                handleDatabaseUser(snapshot, uid, email)
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                showError("Database error: ${exception.message}")
                showLoading(false)
            }
    }

    private fun handleDatabaseUser(snapshot: DataSnapshot, uid: String, email: String) {
        val userData = snapshot.getValue(UserData::class.java)

        if (userData == null) {
            createUserInDatabase(uid, email)
        } else if (!userData.isActive) {
            showError("Account is inactive. Contact admin.")
        } else {
            updateLastLoginAndProceed(uid, email)
        }
    }

    private fun createUserInDatabase(uid: String, email: String) {
        val newUser = UserData(
            uid = uid,
            email = email,
            role = "staff",
            department = "Warehouse",
            isActive = true,
            createdAt = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis(),
            permissions = listOf("view_inventory", "edit_inventory")
        )
        usersRef.child(uid).setValue(newUser)
            .addOnSuccessListener {
                handleSuccessfulLogin(email)
            }
            .addOnFailureListener { e ->
                showError("Failed to create user profile: ${e.message}")
            }
    }

    private fun updateLastLoginAndProceed(uid: String, email: String) {
        usersRef.child(uid).child("lastLogin").setValue(System.currentTimeMillis())
            .addOnSuccessListener {
                handleSuccessfulLogin(email)
            }
            .addOnFailureListener { e ->
                Log.e("Login", "Failed to update last login: ${e.message}")
                handleSuccessfulLogin(email)
            }
    }

    private fun handleSuccessfulLogin(email: String) {
        saveEmailToPreferences(email)
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }

    private fun handleFailedLogin(errorMessage: String?) {
        when {
            errorMessage?.contains("password") == true -> {
                passwordInputLayout.error = "Incorrect password"
            }
            errorMessage?.contains("no user record") == true -> {
                emailInputLayout.error = "No account found with this email"
            }
            else -> showError("Login failed. Please try again.")
        }
    }

    private fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    showSuccess("Password reset email sent")
                } else {
                    showError("Failed to send reset email. Please try again.")
                }
            }
    }

    private fun clearErrors() {
        emailInputLayout.error = null
        passwordInputLayout.error = null
    }

    private fun checkUserSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingDialog.show()
        } else {
            loadingDialog.dismiss()
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

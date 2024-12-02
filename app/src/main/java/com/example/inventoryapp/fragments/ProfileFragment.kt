package com.example.inventoryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.activities.LoginActivity
import com.example.inventoryapp.databinding.FragmentProfileBinding
import com.example.inventoryapp.utils.LoadingDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.appwrite.exceptions.AppwriteException
import io.appwrite.services.Account
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var loadingDialog: LoadingDialog
    private val account = Account(AppwriteManager.getClient())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = LoadingDialog(requireActivity())

        loadUserProfile()
        setupClickListeners()
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                // Get user info from Appwrite
                val user = account.get()

                // Get user details from database
                val doc = AppwriteManager.databases.getDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    documentId = user.id
                )

                binding.apply {
                    userEmail.text = doc.data["email"] as String
                    userRole.text = (doc.data["role"] as String).capitalize()
                }
            } catch (e: AppwriteException) {
                showError("Failed to load profile: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        binding.changePasswordLayout.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.logoutLayout.setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = layoutInflater.inflate(
            com.example.inventoryapp.R.layout.dialog_change_password, null)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Password")
            .setView(dialogBinding)
            .setPositiveButton("Update") { dialog, _ ->
                val newPassword = dialogBinding.findViewById<com.google.android.material.textfield.TextInputEditText>(
                    com.example.inventoryapp.R.id.newPasswordInput).text.toString()

                if (newPassword.length >= 8) {
                    updatePassword(newPassword)
                } else {
                    showError("Password must be at least 8 characters long")
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun updatePassword(newPassword: String) {
        loadingDialog.show()
        lifecycleScope.launch {
            try {
                account.updatePassword(newPassword)
                showSuccess("Password updated successfully")
            } catch (e: AppwriteException) {
                showError("Failed to update password: ${e.message}")
            } finally {
                loadingDialog.dismiss()
            }
        }
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ -> logout() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        loadingDialog.show()
        lifecycleScope.launch {
            try {
                account.deleteSession("current")
                // Clear any local data if needed
                requireActivity().apply {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            } catch (e: AppwriteException) {
                showError("Failed to logout: ${e.message}")
                loadingDialog.dismiss()
            }
        }
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
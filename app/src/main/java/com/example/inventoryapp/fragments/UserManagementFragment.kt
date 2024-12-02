package com.example.inventoryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.R
import com.example.inventoryapp.activities.AddEditUserActivity
import com.example.inventoryapp.adapters.UserAdapter
import com.example.inventoryapp.databinding.FragmentUserManagementBinding
import com.example.inventoryapp.models.UserData
import io.appwrite.Query
import kotlinx.coroutines.launch

class UserManagementFragment : Fragment() {
    private var _binding: FragmentUserManagementBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: UserAdapter
    private var allUsers = listOf<UserData>()
    private var currentFilter = "all"
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupAddButton()
        loadUsers()
    }

    private fun setupRecyclerView() {
        adapter = UserAdapter(
            onUserClick = { user ->
                // Open edit user activity
                startActivity(Intent(requireContext(), AddEditUserActivity::class.java).apply {
                    putExtra("userId", user.id)
                })
            },
            onActiveToggle = { user, isActive ->
                updateUserStatus(user, isActive)
            }
        )

        binding.userList.apply {
            adapter = this@UserManagementFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.lowercase() ?: ""
                filterUsers()
            }
        })
    }

    private fun setupFilters() {
        binding.roleFilter.setOnCheckedChangeListener { group, checkedId ->
            currentFilter = when (checkedId) {
                R.id.filterAdmin -> "admin"
                R.id.filterStaff -> "staff"
                else -> "all"
            }
            filterUsers()
        }
    }

    private fun setupAddButton() {
        binding.addUserFab.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditUserActivity::class.java))
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val response = AppwriteManager.databases.listDocuments(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    queries = listOf(Query.orderDesc("\$createdAt"))
                )

                allUsers = response.documents.map { doc ->
                    UserData(
                        id = doc.id,
                        email = doc.data["email"] as String,
                        role = doc.data["role"] as String,
                        isActive = doc.data["isActive"] as Boolean,
                        lastLogin = doc.data["\$createdAt"] as String
                    )
                }

                updateStats()
                filterUsers()
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Failed to load users: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterUsers() {
        var filtered = allUsers

        if (currentFilter != "all") {
            filtered = filtered.filter { it.role == currentFilter }
        }

        if (searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.email.lowercase().contains(searchQuery)
            }
        }

        adapter.submitList(filtered)
        updateStats()
    }

    private fun updateStats() {
        binding.apply {
            totalStaffCount.text = allUsers.size.toString()
            activeUsersCount.text = allUsers.count { it.isActive }.toString()
        }
    }

    private fun updateUserStatus(user: UserData, isActive: Boolean) {
        lifecycleScope.launch {
            try {
                AppwriteManager.databases.updateDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.USERS,
                    documentId = user.id,
                    data = mapOf("isActive" to isActive)
                )

                val updatedUser = user.copy(isActive = isActive)
                allUsers = allUsers.map { if (it.id == user.id) updatedUser else it }
                filterUsers()

                Toast.makeText(requireContext(),
                    "User status updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Failed to update user status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
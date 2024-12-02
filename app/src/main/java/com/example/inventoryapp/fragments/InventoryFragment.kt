package com.example.inventoryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.R
import com.example.inventoryapp.activities.ItemDetailsActivity
import com.example.inventoryapp.activities.AddEditItemActivity
import com.example.inventoryapp.activities.DashboardActivity
import com.example.inventoryapp.adapters.InventoryAdapter
import com.example.inventoryapp.databinding.FragmentInventoryBinding
import com.example.inventoryapp.models.Category
import com.example.inventoryapp.models.InventoryItem
import com.example.inventoryapp.models.Warehouse
import com.example.inventoryapp.utils.UiUtils
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.appwrite.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class InventoryFragment : Fragment() {
    private var _binding: FragmentInventoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: InventoryAdapter
    private var warehouses = listOf<Warehouse>()
    private var categories = listOf<Category>()
    private var allItems = listOf<InventoryItem>()

    private var searchJob: Job? = null
    private var selectedWarehouse: Warehouse? = null
    private var selectedCategory: Category? = null
    private var selectedStatus: String? = null
    private var currentSortOption = SortOption.NAME

    override fun onResume() {
        super.onResume()
        loadInitialData() // Refresh data when coming back to fragment
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInventoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity() as? DashboardActivity)?.let { homeActivity ->
            homeActivity.findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        }
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadInitialData()
    }

    private fun setupViews() {
        setupRecyclerView()
        setupSearch()
        setupFilters()
        setupSwipeRefresh()
        setupAddButton()
    }

    private fun setupRecyclerView() {
        adapter = InventoryAdapter(warehouses, categories)
        binding.inventoryRecyclerView.apply {
            adapter = this@InventoryFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        adapter.onItemClick = { item ->
            try {
                startActivity(
                    Intent(requireContext(), ItemDetailsActivity::class.java)
                        .putExtra("itemId", item.id)
                )
            } catch (e: Exception) {
                Log.e("InventoryFragment", "Error opening details: ${e.message}")
                showError("Unable to open item details")
            }
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = lifecycleScope.launch {
                    delay(300) // Debounce
                    filterAndDisplayItems()
                }
            }
        })
    }

    private fun setupFilters() {
        binding.filterWarehouse.setOnClickListener { showWarehouseFilter() }
        binding.filterCategory.setOnClickListener { showCategoryFilter() }
        binding.filterStock.setOnClickListener { showStockLevelFilter() }
        binding.filterButton.setOnClickListener { showSortOptions() }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadInitialData()
        }
    }

    private fun setupAddButton() {
        binding.addItemFab.setOnClickListener {
            startActivity(Intent(requireContext(), AddEditItemActivity::class.java))
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                loadWarehousesAndCategories()
                loadInventoryItems()
            } catch (e: Exception) {
                Log.e("InventoryFragment", "Error loading data: ${e.message}")
                showError("Failed to load data. Please try again.")
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadWarehousesAndCategories() {
        try {
            val warehousesResponse = AppwriteManager.databases.listDocuments(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.WAREHOUSES
            )
            warehouses = warehousesResponse.documents.map { doc ->
                Warehouse(
                    id = doc.id,
                    name = doc.data["name"] as String,
                    location = doc.data["location"] as String,
                    address = doc.data["address"] as? String,
                    floors = doc.data["floors"] as List<String>,
                    sections = doc.data["sections"] as List<String>
                )
            }

            val categoriesResponse = AppwriteManager.databases.listDocuments(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.CATEGORIES
            )
            categories = categoriesResponse.documents.map { doc ->
                Category(
                    id = doc.id,
                    name = doc.data["name"] as String,
                    description = doc.data["description"] as? String
                )
            }

            adapter = InventoryAdapter(warehouses, categories)
            binding.inventoryRecyclerView.adapter = adapter
            setupItemClickListener()
        } catch (e: Exception) {
            Log.e("InventoryFragment", "Error loading refs: ${e.message}")
            throw e
        }
    }

    private fun setupItemClickListener() {
        adapter.onItemClick = { item ->
            startActivity(
                Intent(requireContext(), ItemDetailsActivity::class.java)
                    .putExtra("itemId", item.id)
            )
        }
    }

    private suspend fun loadInventoryItems() {
        try {
            val queries = mutableListOf<String>()

            // Handle warehouse filter
            selectedWarehouse?.let {
                queries.add(Query.equal("warehouseId", it.id))
            }

            // Handle category filter
            selectedCategory?.let {
                queries.add(Query.equal("categoryId", it.id))
            }

            // Add sort option
            when (currentSortOption) {
                SortOption.NAME -> queries.add(Query.orderAsc("name"))
                SortOption.STOCK -> queries.add(Query.orderAsc("currentStock"))
                SortOption.PRICE -> queries.add(Query.orderDesc("price"))
            }

            val response = AppwriteManager.databases.listDocuments(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                queries = queries
            )

            // First map to InventoryItem objects
            var items = response.documents.map { doc ->
                InventoryItem(
                    id = doc.id,
                    name = doc.data["name"] as String,
                    description = doc.data["description"] as? String,
                    categoryId = doc.data["categoryId"] as String,
                    currentStock = (doc.data["currentStock"] as Number).toInt(),
                    minStock = (doc.data["minStock"] as Number).toInt(),
                    price = (doc.data["price"] as Number).toFloat(),
                    warehouseId = doc.data["warehouseId"] as String,
                    floor = doc.data["floor"] as? String,
                    section = doc.data["section"] as? String,
                    status = doc.data["status"] as String,
                    sku = doc.data["sku"] as String
                )
            }

            // Then apply stock level filtering
            when (selectedStatus) {
                "out" -> items = items.filter { it.currentStock == 0 }
                "low" -> items = items.filter { it.currentStock <= it.minStock }
                "ok" -> items = items.filter { it.currentStock > it.minStock }
            }

            allItems = items
            filterAndDisplayItems()
        } catch (e: Exception) {
            Log.e("InventoryFragment", "Error loading items: ${e.message}")
            throw e
        }
    }

    private fun filterAndDisplayItems() {
        val searchQuery = binding.searchInput.text?.toString()?.lowercase() ?: ""

        val filteredItems = allItems.filter { item ->
            if (searchQuery.isBlank()) true
            else {
                item.name.lowercase().contains(searchQuery) ||
                        item.sku.lowercase().contains(searchQuery)
            }
        }

        updateItemsList(filteredItems)
    }

    private fun updateItemsList(items: List<InventoryItem>) {
        try {
            adapter.updateItems(items)
            binding.inventoryCount.text = "${items.size} items"
            binding.emptyState.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE

            // Update empty state message based on filters
            if (items.isEmpty()) {
                val reason = when {
                    binding.searchInput.text?.isNotBlank() == true -> "No items match your search"
                    selectedWarehouse != null -> "No items in selected warehouse"
                    selectedCategory != null -> "No items in selected category"
                    selectedStatus != null -> "No items with selected status"
                    else -> "No items found"
                }
                binding.emptyState.findViewById<android.widget.TextView>(R.id.emptyStateMessage)?.text = reason
            }
        } catch (e: Exception) {
            Log.e("InventoryFragment", "Error updating list: ${e.message}")
            showError("Failed to update item list")
        }
    }

    private fun showWarehouseFilter() {
        val items = listOf("All Warehouses") + warehouses.map { it.name }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Warehouse")
            .setItems(items.toTypedArray()) { _, which ->
                selectedWarehouse = if (which == 0) null else warehouses[which - 1]
                binding.filterWarehouse.isChecked = selectedWarehouse != null
                loadInitialData()
            }
            .show()
    }

    private fun showCategoryFilter() {
        val items = listOf("All Categories") + categories.map { it.name }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Category")
            .setItems(items.toTypedArray()) { _, which ->
                selectedCategory = if (which == 0) null else categories[which - 1]
                binding.filterCategory.isChecked = selectedCategory != null
                loadInitialData()
            }
            .show()
    }

    private fun showStockLevelFilter() {
        val options = arrayOf("All", "Low Stock", "Out of Stock", "Well Stocked")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filter by Stock Level")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> selectedStatus = null
                    1 -> selectedStatus = "low"
                    2 -> selectedStatus = "out"
                    3 -> selectedStatus = "ok"
                }
                binding.filterStock.isChecked = selectedStatus != null
                loadInitialData()
            }
            .show()
    }

    private fun showSortOptions() {
        val options = arrayOf("Name", "Stock Level", "Price")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sort By")
            .setSingleChoiceItems(options, currentSortOption.ordinal) { dialog, which ->
                currentSortOption = SortOption.values()[which]
                loadInitialData()
                dialog.dismiss()
            }
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    private fun showError(message: String) {
        UiUtils.showError(binding.root, message)
    }

    private enum class SortOption {
        NAME, STOCK, PRICE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
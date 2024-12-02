package com.example.inventoryapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.R
import com.example.inventoryapp.adapters.DashboardStatsAdapter
import com.example.inventoryapp.adapters.LowStockAdapter
import com.example.inventoryapp.databinding.ActivityDashboardBinding
import com.example.inventoryapp.fragments.InventoryFragment
import com.example.inventoryapp.fragments.ProfileFragment
import com.example.inventoryapp.fragments.UserManagementFragment
import com.example.inventoryapp.models.DashboardStat
import com.example.inventoryapp.models.LowStockItem
import com.example.inventoryapp.models.Warehouse
import com.example.inventoryapp.managers.RoleManager
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private val statsAdapter = DashboardStatsAdapter()
    private val lowStockAdapter = LowStockAdapter()
    private var selectedWarehouse: Warehouse? = null
    private var warehouses = listOf<Warehouse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerViews()
        setupWarehouseSelector()
        setupSwipeRefresh()
        setupQuickActions()
        setupBottomNavigation()
        setupErrorView()

        // Initial load
        loadDashboardData()
    }

    private fun setupBottomNavigation() {
        lifecycleScope.launch {
            // Check if user is admin
            if (RoleManager.isAdmin()) {
                // For admin, we'll include the admin navigation case
                setupAdminBottomBar()
            } else {
                // For regular users, just setup normal navigation
                setupRegularBottomBar()
            }
        }
    }

    private fun setupAdminBottomBar() {
        binding.bottomBar.onItemSelected = { position ->
            when (position) {
                0 -> {
                    // Dashboard - show main content, hide fragment container
                    binding.apply {
                        swipeRefresh.visibility = View.VISIBLE
                        fragmentContainer.visibility = View.GONE
                        errorView.root.visibility = View.GONE
                        warehouseSelector.visibility = View.VISIBLE
                        statsRecyclerView.visibility = View.VISIBLE
                        lowStockRecyclerView.visibility = View.VISIBLE
                        toolbar.visibility = View.VISIBLE
                    }
                    loadDashboardData()
                    true
                }
                1 -> {
                    // Inventory
                    binding.apply {
                        swipeRefresh.visibility = View.GONE
                        fragmentContainer.visibility = View.VISIBLE
                        errorView.root.visibility = View.GONE
                        warehouseSelector.visibility = View.GONE
                        statsRecyclerView.visibility = View.GONE
                        lowStockRecyclerView.visibility = View.GONE
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, InventoryFragment())
                        .commit()
                    true
                }
                2 -> {
                    // Profile
                    binding.apply {
                        swipeRefresh.visibility = View.GONE
                        fragmentContainer.visibility = View.VISIBLE
                        warehouseSelector.visibility = View.GONE
                        statsRecyclerView.visibility = View.GONE
                        lowStockRecyclerView.visibility = View.GONE
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProfileFragment())
                        .commit()
                    true
                }
                3 -> {
                    // Admin - only accessible if user is admin
                    binding.apply {
                        swipeRefresh.visibility = View.GONE
                        fragmentContainer.visibility = View.VISIBLE
                        errorView.root.visibility = View.GONE
                        warehouseSelector.visibility = View.GONE
                        statsRecyclerView.visibility = View.GONE
                        lowStockRecyclerView.visibility = View.GONE
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, UserManagementFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Set initial selection
        binding.bottomBar.setActiveItem(0)
    }

    private fun setupRegularBottomBar() {
        binding.bottomBar.onItemSelected = { position ->
            when (position) {
                0 -> {
                    // Dashboard - show main content, hide fragment container
                    binding.apply {
                        swipeRefresh.visibility = View.VISIBLE
                        fragmentContainer.visibility = View.GONE
                        errorView.root.visibility = View.GONE
                        warehouseSelector.visibility = View.VISIBLE
                        statsRecyclerView.visibility = View.VISIBLE
                        lowStockRecyclerView.visibility = View.VISIBLE
                        toolbar.visibility = View.VISIBLE
                    }
                    loadDashboardData()
                    true
                }
                1 -> {
                    // Inventory
                    binding.apply {
                        swipeRefresh.visibility = View.GONE
                        fragmentContainer.visibility = View.VISIBLE
                        errorView.root.visibility = View.GONE
                        warehouseSelector.visibility = View.GONE
                        statsRecyclerView.visibility = View.GONE
                        lowStockRecyclerView.visibility = View.GONE
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, InventoryFragment())
                        .commit()
                    true
                }
                2 -> {
                    // Profile
                    binding.apply {
                        swipeRefresh.visibility = View.GONE
                        fragmentContainer.visibility = View.VISIBLE
                        warehouseSelector.visibility = View.GONE
                        statsRecyclerView.visibility = View.GONE
                        lowStockRecyclerView.visibility = View.GONE
                    }
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }

        // Set initial selection
        binding.bottomBar.setActiveItem(0)
    }

    override fun onBackPressed() {
        when {
            binding.fragmentContainer.visibility == View.VISIBLE -> {
                // If showing a fragment, return to dashboard
                binding.bottomBar.setActiveItem(0)
                binding.apply {
                    fragmentContainer.visibility = View.GONE
                    swipeRefresh.visibility = View.VISIBLE
                    warehouseSelector.visibility = View.VISIBLE
                    statsRecyclerView.visibility = View.VISIBLE
                    lowStockRecyclerView.visibility = View.VISIBLE
                    toolbar.visibility = View.VISIBLE
                }
                loadDashboardData()
            }
            else -> super.onBackPressed()
        }
    }

    private fun setupRecyclerViews() {
        binding.statsRecyclerView.apply {
            adapter = statsAdapter
            layoutManager = GridLayoutManager(this@DashboardActivity, 2)
        }

        binding.lowStockRecyclerView.apply {
            adapter = lowStockAdapter
            layoutManager = LinearLayoutManager(this@DashboardActivity)
        }

        lowStockAdapter.onItemClick = { item ->
            startActivity(
                Intent(this, AddEditItemActivity::class.java)
                    .putExtra("itemId", item.id)
            )
        }
    }

    private fun setupWarehouseSelector() {
        binding.warehouseSelector.setOnClickListener {
            if (warehouses.isNotEmpty()) {
                showWarehouseDialog()
            } else {
                showError("No warehouses available") {
                    loadWarehouses()
                }
            }
        }
        loadWarehouses()
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }
    }

    private fun setupQuickActions() {
        binding.addAction.setOnClickListener {
            startActivity(Intent(this, AddEditItemActivity::class.java))
        }

        binding.scanAction.setOnClickListener {
            // TODO: Implement scan functionality if needed
        }
    }

    private fun setupErrorView() {
        binding.errorView.retryButton.setOnClickListener {
            binding.errorView.root.visibility = View.GONE
            loadDashboardData()
        }
    }

    private fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    private fun showError(message: String, retryAction: (() -> Unit)? = null) {
        binding.errorView.apply {
            root.visibility = View.VISIBLE
            errorText.text = message
            retryButton.setOnClickListener {
                root.visibility = View.GONE
                retryAction?.invoke()
            }
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                showLoading(true)
                binding.errorView.root.visibility = View.GONE

                // Load stats
                val stats = loadStats()
                statsAdapter.updateStats(stats)

                // Load low stock items
                val lowStockItems = loadLowStockItems()
                lowStockAdapter.updateItems(lowStockItems)

                // Update visibility
                binding.lowStockEmptyMessage.visibility =
                    if (lowStockItems.isEmpty()) View.VISIBLE else View.GONE

            } catch (e: Exception) {
                showError("Failed to load dashboard: ${e.message}") {
                    loadDashboardData()
                }
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadStats(): List<DashboardStat> {
        val databases = AppwriteManager.databases
        val warehouseId = selectedWarehouse?.id
        val queries = mutableListOf(Query.equal("status", "Active"))
        warehouseId?.let { queries.add(Query.equal("warehouseId", listOf(it))) }

        val items = databases.listDocuments(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
            queries = queries
        )

        val totalItems = items.total
        val totalValue = items.documents.sumOf {
            val price = (it.data["price"] as? Number)?.toDouble() ?: 0.0
            val stock = (it.data["currentStock"] as? Number)?.toInt() ?: 0
            price * stock
        }

        // Fix for low stock count calculation
        val lowStockCount = items.documents.count {
            val currentStock = (it.data["currentStock"] as? Number)?.toInt() ?: 0
            val minStock = (it.data["minStock"] as? Number)?.toInt() ?: 0
            currentStock < minStock  // Changed from <= to < for strict low stock definition
        }

        return listOf(
            DashboardStat("Total Items", totalItems.toString(), R.drawable.ic_inventory),
            DashboardStat(
                "Total Value",
                NumberFormat.getCurrencyInstance(Locale.US).format(totalValue),
                R.drawable.ic_money
            ),
            DashboardStat(
                "Low Stock Items",
                lowStockCount.toString(),
                R.drawable.ic_warning,
                warning = lowStockCount > 0
            )
        )
    }

    private suspend fun loadLowStockItems(): List<LowStockItem> {
        val databases = AppwriteManager.databases
        val warehouseId = selectedWarehouse?.id
        val queries = mutableListOf(Query.equal("status", "Active")) // Only active items
        warehouseId?.let { queries.add(Query.equal("warehouseId", listOf(it))) }

        val items = databases.listDocuments(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
            queries = queries
        )

        return items.documents.mapNotNull {
            val currentStock = (it.data["currentStock"] as? Number)?.toInt() ?: return@mapNotNull null
            val minStock = (it.data["minStock"] as? Number)?.toInt() ?: return@mapNotNull null
            val sku = it.data["sku"] as? String ?: "Unknown SKU"

            if (currentStock < minStock) {  // Changed from <= to
                LowStockItem(
                    id = it.id,
                    name = it.data["name"] as? String ?: "Unknown Item",
                    currentStock = currentStock,
                    minStock = minStock,
                    warehouseName = selectedWarehouse?.name ?: "All Warehouses",
                    floor = it.data["floor"] as? String ?: "",
                    location = it.data["section"] as? String ?: "",
                    sku = sku
                )
            } else null
        }
    }

    private fun loadWarehouses() {
        lifecycleScope.launch {
            try {
                val response = AppwriteManager.databases.listDocuments(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.WAREHOUSES
                )

                warehouses = response.documents.map {
                    Warehouse(
                        id = it.id,
                        name = it.data["name"] as String,
                        location = it.data["location"] as String,
                        address = it.data["address"] as? String,
                        floors = it.data["floors"] as List<String>,
                        sections = it.data["sections"] as List<String>
                    )
                }
                binding.warehouseSelector.text = "All Warehouses"
            } catch (e: Exception) {
                showError("Failed to load warehouses: ${e.message}")
            }
        }
    }

    private fun showWarehouseDialog() {
        val items = listOf("All Warehouses") + warehouses.map { it.name }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Warehouse")
            .setItems(items.toTypedArray()) { _, which ->
                selectedWarehouse = if (which == 0) null else warehouses[which - 1]
                binding.warehouseSelector.text = items[which]
                loadDashboardData()
            }
            .show()
    }
}
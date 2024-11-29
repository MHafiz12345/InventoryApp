package com.example.inventoryapp.activities

import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.R
import com.example.inventoryapp.adapters.DashboardStatAdapter
import com.example.inventoryapp.adapters.LowStockAdapter
import com.example.inventoryapp.base.BaseActivity
import com.example.inventoryapp.databinding.ActivityDashboardBinding
import com.example.inventoryapp.models.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DashboardActivity : BaseActivity<ActivityDashboardBinding>() {
    private val firestore = FirebaseFirestore.getInstance()
    private val statAdapter = DashboardStatAdapter()
    private val lowStockAdapter = LowStockAdapter()

    private var selectedWarehouse: Warehouse? = null

    override fun getLayoutResId(): Int = R.layout.activity_dashboard

    override fun inflateViewBinding(): ActivityDashboardBinding =
        ActivityDashboardBinding.inflate(layoutInflater)

    override fun initializeViews() {
        setupRecyclerViews()
        setupClickListeners()
        loadWarehouses() // Initial load
    }

    private fun setupRecyclerViews() {
        binding.apply {
            // Stats RecyclerView
            statsRecyclerView.apply {
                layoutManager = LinearLayoutManager(
                    this@DashboardActivity,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = statAdapter
            }

            // Low Stock RecyclerView
            lowStockRecyclerView.apply {
                layoutManager = LinearLayoutManager(this@DashboardActivity)
                adapter = lowStockAdapter
                isNestedScrollingEnabled = false
            }
        }
    }

    // In DashboardActivity.kt, replace the bottom bar setup with:

    private fun setupClickListeners() {
        binding.apply {
            // Warehouse selector
            warehouseSelector.setOnClickListener {
                showWarehouseSelector()
            }

            // Add Item action
            addAction.setOnClickListener {
                selectedWarehouse?.let { warehouse ->
                    startActivity(
                        Intent(
                            this@DashboardActivity,
                            AddEditItemActivity::class.java
                        ).apply {
                            putExtra("warehouse_id", warehouse.id)
                        })
                } ?: showError("Please select a warehouse first")
            }

            // Scan action
            scanAction.setOnClickListener {
                showError("Scan functionality coming soon")
            }

            // Swipe refresh
            swipeRefresh.setOnRefreshListener {
                selectedWarehouse?.let { loadWarehouseInventory(it.id) }
                    ?: loadWarehouses()
            }

            // Bottom navigation
            bottomBar.onItemSelected = { position ->
                when (position) {
                    0 -> {} // Dashboard (current)
                    1 -> {} // Implement other navigation items
                    // Add more cases as needed
                }
            }
        }
    }

    private fun loadWarehouses() {
        showLoading(true)
        firestore.collection("warehouses")
            .get()
            .addOnSuccessListener { snapshot ->
                val warehouses = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Warehouse::class.java)?.apply { id = doc.id }
                }

                if (warehouses.isNotEmpty()) {
                    // Select first warehouse if none selected
                    if (selectedWarehouse == null) {
                        selectedWarehouse = warehouses.first()
                        binding.warehouseSelector.text = warehouses.first().name
                        loadWarehouseInventory(warehouses.first().id)
                    }
                } else {
                    showError("No warehouses available")
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                showError("Failed to load warehouses: ${e.message}")
                showLoading(false)
            }
    }

    private fun showWarehouseSelector() {
        showLoading(true)
        firestore.collection("warehouses")
            .get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)
                val warehouses = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Warehouse::class.java)?.apply { id = doc.id }
                }

                if (warehouses.isEmpty()) {
                    showError("No warehouses available")
                    return@addOnSuccessListener
                }

                MaterialAlertDialogBuilder(this)
                    .setTitle("Select Warehouse")
                    .setSingleChoiceItems(
                        warehouses.map { it.name }.toTypedArray(),
                        warehouses.indexOfFirst { it.id == selectedWarehouse?.id }
                    ) { dialog, which ->
                        val selected = warehouses[which]
                        selectedWarehouse = selected
                        binding.warehouseSelector.text = selected.name
                        loadWarehouseInventory(selected.id)
                        dialog.dismiss()
                    }
                    .show()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Failed to load warehouses: ${e.message}")
            }
    }

    private fun loadWarehouseInventory(warehouseId: String) {
        showLoading(true)
        binding.swipeRefresh.isRefreshing = true

        // Query all items for this warehouse across all categories
        firestore.collectionGroup("items")
            .whereEqualTo("location.warehouseId", warehouseId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val items = snapshot.documents.mapNotNull {
                    it.toObject(InventoryItem::class.java)
                }
                updateDashboard(items)
                binding.swipeRefresh.isRefreshing = false
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showError("Failed to load inventory: ${e.message}")
                binding.swipeRefresh.isRefreshing = false
                showLoading(false)
            }
    }

    private fun updateDashboard(items: List<InventoryItem>) {
        binding.apply {
            if (items.isEmpty()) {
                // Show empty state
                statsRecyclerView.visibility = View.GONE
                lowStockCard.visibility = View.GONE
                quickActionsGrid.visibility = View.GONE
                // Instead of using a separate empty state layout, use the lowStockEmptyMessage
                lowStockEmptyMessage.visibility = View.VISIBLE
                lowStockEmptyMessage.text = "No items found in this warehouse"
            } else {
                // Show content state
                statsRecyclerView.visibility = View.VISIBLE
                lowStockCard.visibility = View.VISIBLE
                quickActionsGrid.visibility = View.VISIBLE

                // Update statistics
                val stats = listOf(
                    DashboardStat(
                        title = "Total Items",
                        value = items.size.toString(),
                        icon = R.drawable.ic_inventory
                    ),
                    DashboardStat(
                        title = "Low Stock",
                        value = items.count { it.currentStock <= it.minStock }.toString(),
                        icon = R.drawable.ic_warning,
                        warning = true
                    ),
                    DashboardStat(
                        title = "Total Value",
                        value = "$%.2f".format(items.sumOf { it.price * it.currentStock }),
                        icon = R.drawable.ic_money
                    )
                )
                statAdapter.updateStats(stats)

                // Update low stock items
                val lowStockItems = items.filter { it.currentStock <= it.minStock }
                if (lowStockItems.isEmpty()) {
                    lowStockEmptyMessage.visibility = View.VISIBLE
                    lowStockRecyclerView.visibility = View.GONE
                    lowStockEmptyMessage.text = "No low stock items to display"
                } else {
                    lowStockEmptyMessage.visibility = View.GONE
                    lowStockRecyclerView.visibility = View.VISIBLE
                    lowStockAdapter.updateItems(lowStockItems.sortedBy { it.currentStock })
                }

                // Calculate and show additional metrics if needed
                val totalCategories = items.map { it.category }.distinct().size
                val averageValue = if (items.isNotEmpty()) {
                    items.sumOf { it.price * it.currentStock } / items.size
                } else 0.0
            }

            // Show/hide the refresh indicator
            swipeRefresh.isRefreshing = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to dashboard
        selectedWarehouse?.let { loadWarehouseInventory(it.id) }
    }
}
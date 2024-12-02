package com.example.inventoryapp.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.R
import com.example.inventoryapp.activities.AddEditItemActivity
import com.example.inventoryapp.adapters.DashboardStatsAdapter
import com.example.inventoryapp.adapters.LowStockAdapter
import com.example.inventoryapp.databinding.FragmentDashboardBinding
import com.example.inventoryapp.models.DashboardStat
import com.example.inventoryapp.models.LowStockItem
import com.example.inventoryapp.models.Warehouse
import com.example.inventoryapp.utils.UiUtils
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val statsAdapter = DashboardStatsAdapter()
    private val lowStockAdapter = LowStockAdapter()
    private var selectedWarehouse: Warehouse? = null
    private var warehouses = listOf<Warehouse>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupWarehouseSelector()
        setupSwipeRefresh()
        setupQuickActions()

        // Initial load
        loadDashboardData()
    }

    private fun setupRecyclerViews() {
        binding.statsRecyclerView.apply {
            adapter = statsAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }

        binding.lowStockRecyclerView.apply {
            adapter = lowStockAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupWarehouseSelector() {
        binding.warehouseSelector.setOnClickListener {
            if (warehouses.isNotEmpty()) {
                showWarehouseDialog()
            } else {
                UiUtils.showError(binding.root, "No warehouses available") {
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
            startActivity(Intent(requireContext(), AddEditItemActivity::class.java))
        }
    }

    private fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                showLoading(true)

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
                UiUtils.showError(binding.root, "Failed to load dashboard: ${e.message}") {
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
        val queries = mutableListOf(Query.equal("status", "Active")) // Only active items
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
        val lowStockCount = items.documents.count {
            val currentStock = (it.data["currentStock"] as? Number)?.toInt() ?: 0
            val minStock = (it.data["minStock"] as? Number)?.toInt() ?: 0
            currentStock <= minStock
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

            if (currentStock <= minStock) {
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
                UiUtils.showError(binding.root, "Failed to load warehouses: ${e.message}")
            }
        }
    }

    private fun showWarehouseDialog() {
        val items = listOf("All Warehouses") + warehouses.map { it.name }
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Warehouse")
            .setItems(items.toTypedArray()) { _, which ->
                selectedWarehouse = if (which == 0) null else warehouses[which - 1]
                binding.warehouseSelector.text = items[which] // Update the warehouse selector text
                loadDashboardData() // Reload dashboard data based on the selected warehouse
            }
            .show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package com.example.inventoryapp.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.R
import com.example.inventoryapp.adapters.ActivityLogAdapter
import com.example.inventoryapp.adapters.StockMovementAdapter
import com.example.inventoryapp.databinding.ActivityItemDetailsBinding
import com.example.inventoryapp.databinding.DialogStockAdjustmentBinding
import com.example.inventoryapp.models.ActivityLog
import com.example.inventoryapp.models.InventoryItem
import com.example.inventoryapp.models.StockMovement
import com.example.inventoryapp.models.Warehouse
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.appwrite.ID
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class ItemDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityItemDetailsBinding
    private var itemId: String? = null
    private var item: InventoryItem? = null
    private var warehouses = listOf<Warehouse>()
    private lateinit var stockMovementAdapter: StockMovementAdapter
    private lateinit var activityLogAdapter: ActivityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemId = intent.getStringExtra("itemId")
        setupViews()
        loadInitialData()
    }

    private fun setupViews() {
        setupToolbar()
        setupRecyclerViews()
        setupButtons()
        showLoading(true)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerViews() {
        // Setup Stock Movements RecyclerView
        stockMovementAdapter = StockMovementAdapter()
        binding.stockMovementsRecyclerView.apply {
            adapter = stockMovementAdapter
            layoutManager = LinearLayoutManager(this@ItemDetailsActivity)
        }

        // Setup Activity Logs RecyclerView
        activityLogAdapter = ActivityLogAdapter()
        binding.activityLogsRecyclerView.apply {
            adapter = activityLogAdapter
            layoutManager = LinearLayoutManager(this@ItemDetailsActivity)
        }
    }

    private fun setupButtons() {
        binding.editFab.setOnClickListener {
            startActivity(Intent(this, AddEditItemActivity::class.java).apply {
                putExtra("itemId", itemId)
            })
        }

        binding.adjustStockButton.setOnClickListener {
            item?.let { showAdjustStockDialog(it) }
        }

        binding.deleteButton.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun loadInitialData() {
        lifecycleScope.launch {
            try {
                loadWarehouses()
                loadItemDetails()
                loadStockMovements()
                loadActivityLogs()
                showLoading(false)
            } catch (e: Exception) {
                Log.e("ItemDetails", "Error loading data: ${e.message}")
                showError("Failed to load item details")
            }
        }
    }

    private suspend fun loadWarehouses() {
        val response = AppwriteManager.databases.listDocuments(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.WAREHOUSES
        )
        warehouses = response.documents.map { doc ->
            Warehouse(
                id = doc.id,
                name = doc.data["name"] as String,
                location = doc.data["location"] as String,
                address = doc.data["address"] as? String,
                floors = doc.data["floors"] as List<String>,
                sections = doc.data["sections"] as List<String>
            )
        }
    }

    private suspend fun loadItemDetails() {
        itemId?.let { id ->
            val doc = AppwriteManager.databases.getDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                documentId = id
            )

            item = InventoryItem(
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

            updateUI()
        }
    }

    private suspend fun loadStockMovements() {
        itemId?.let { id ->
            val response = AppwriteManager.databases.listDocuments(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.STOCK_MOVEMENTS,
                queries = listOf(
                    Query.equal("itemId", id),
                    Query.orderDesc("\$createdAt")
                )
            )

            val movements = response.documents.map { doc ->
                StockMovement(
                    id = doc.id,
                    itemId = doc.data["itemId"] as String,
                    warehouseId = doc.data["warehouseId"] as String,
                    quantity = (doc.data["quantity"] as Number).toInt(),
                    type = doc.data["type"] as String,
                    reason = doc.data["reason"] as? String,
                    userId = doc.data["userId"] as String,
                    timestamp = doc.data["\$createdAt"] as String
                )
            }

            stockMovementAdapter.submitList(movements)
        }
    }

    private fun getStatusColor(status: String): ColorStateList {
        return when (status) {
            "Active" -> ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_active))  // Green for Active
            "Inactive" -> ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_inactive))  // Orange for Inactive
            "Discontinued" -> ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_discontinued))  // Red for Discontinued
            else -> ColorStateList.valueOf(ContextCompat.getColor(this, R.color.dark_gray))  // Default to dark gray
        }
    }
    private suspend fun loadActivityLogs() {
        itemId?.let { id ->
            val response = AppwriteManager.databases.listDocuments(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.ACTIVITY_LOGS,
                queries = listOf(
                    Query.equal("itemId", id),
                    Query.orderDesc("\$createdAt")
                )
            )

            val logs = response.documents.map { doc ->
                ActivityLog(
                    id = doc.id,
                    userId = doc.data["userId"] as String,
                    itemId = doc.data["itemId"] as String,
                    actionType = doc.data["actionType"] as String,
                    oldValue = doc.data["oldValue"] as? String,
                    newValue = doc.data["newValue"] as? String,
                    details = doc.data["details"] as? String,
                    timestamp = doc.data["\$createdAt"] as String
                )
            }

            activityLogAdapter.submitList(logs)
        }
    }

    private fun updateUI() {
        item?.let { item ->
            // Basic Info
            binding.itemName.text = item.name
            binding.skuText.text = item.sku
            binding.statusChip.text = item.status
            binding.statusChip.backgroundTintList = getStatusColor(item.status)
            binding.priceText.text = "$%.2f".format(item.price) // Change "$" to your currency symbol

            // Stock Info
            binding.currentStockText.text = item.currentStock.toString()
            binding.minStockText.text = item.minStock.toString()

            // Location Info
            val warehouse = warehouses.find { it.id == item.warehouseId }
            binding.warehouseText.text = warehouse?.name ?: "Unknown"
            binding.floorText.text = "Floor: ${item.floor ?: "N/A"}"
            binding.sectionText.text = "Section: ${item.section ?: "N/A"}"

            // Stock warning
            if (item.currentStock <= item.minStock) {
                binding.currentStockText.setTextColor(getColor(android.R.color.holo_red_dark))
            }
        }
    }

    private fun showAdjustStockDialog(item: InventoryItem) {
        val dialogBinding = DialogStockAdjustmentBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.apply {
            currentStockText.text = "Current Stock: ${item.currentStock}"
            stockInput.setText(item.currentStock.toString())

            increaseButton.setOnClickListener {
                val current = stockInput.text.toString().toIntOrNull() ?: 0
                stockInput.setText((current + 1).toString())
            }

            decreaseButton.setOnClickListener {
                val current = stockInput.text.toString().toIntOrNull() ?: 0
                if (current > 0) stockInput.setText((current - 1).toString())
            }

            updateButton.setOnClickListener {
                val newStock = stockInput.text.toString().toIntOrNull()
                if (newStock != null && newStock >= 0) {
                    val reason = reasonInput.text.toString()
                    updateStock(item, newStock, reason)
                    dialog.dismiss()
                } else {
                    Toast.makeText(this@ItemDetailsActivity, "Invalid stock value", Toast.LENGTH_SHORT).show()
                }
            }

            cancelButton.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }

    private fun updateStock(item: InventoryItem, newStock: Int, reason: String) {
        lifecycleScope.launch {
            try {
                val stockDiff = newStock - item.currentStock
                val movementType = if (stockDiff > 0) "INCREASE" else "DECREASE"

                // Update item stock
                AppwriteManager.databases.updateDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                    documentId = item.id,
                    data = mapOf("currentStock" to newStock)
                )

                // Create stock movement record
                createStockMovement(item.id, item.warehouseId, stockDiff, movementType, reason)

                // Create activity log
                createActivityLog(
                    item.id,
                    "STOCK_UPDATE",
                    item.currentStock.toString(),
                    newStock.toString(),
                    "Stock updated from ${item.currentStock} to $newStock"
                )

                // Reload data
                loadItemDetails()
                loadStockMovements()
                loadActivityLogs()

                Toast.makeText(this@ItemDetailsActivity, "Stock updated successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("ItemDetails", "Error updating stock: ${e.message}")
                showError("Failed to update stock")
            }
        }
    }

    private suspend fun createStockMovement(
        itemId: String,
        warehouseId: String,
        quantity: Int,
        type: String,
        reason: String
    ) {
        val movementData = mapOf(
            "itemId" to itemId,
            "warehouseId" to warehouseId,
            "quantity" to quantity,
            "type" to type,
            "reason" to reason,
            "userId" to "system"
        )

        AppwriteManager.databases.createDocument(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.STOCK_MOVEMENTS,
            documentId = ID.unique(),
            data = movementData
        )
    }

    private suspend fun createActivityLog(
        itemId: String,
        actionType: String,
        oldValue: String? = null,
        newValue: String? = null,
        details: String? = null
    ) {
        val logData = mapOf(
            "itemId" to itemId,
            "userId" to "system",
            "actionType" to actionType,
            "oldValue" to (oldValue ?: ""),
            "newValue" to (newValue ?: ""),
            "details" to (details ?: "")
        )

        AppwriteManager.databases.createDocument(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.ACTIVITY_LOGS,
            documentId = ID.unique(),
            data = logData
        )
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete this item? This action cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> deleteItem() }
            .show()
    }

    private fun deleteItem() {
        lifecycleScope.launch {
            try {
                item?.let { item ->
                    // First create the log
                    createActivityLog(
                        itemId = item.id,
                        actionType = "DELETE",
                        details = "Item deleted: ${item.name}"
                    )

                    // Only delete the specific document
                    AppwriteManager.databases.deleteDocument(
                        databaseId = AppwriteManager.DATABASE_ID,
                        collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                        documentId = item.id
                    )

                    Toast.makeText(this@ItemDetailsActivity, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("ItemDetails", "Error deleting item: ${e.message}")
                showError("Failed to delete item")
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.loadingProgress.visibility = if (show) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadInitialData() // Refresh data when returning to activity
    }
}
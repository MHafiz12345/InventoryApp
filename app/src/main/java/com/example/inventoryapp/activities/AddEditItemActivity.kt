package com.example.inventoryapp.activities

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.inventoryapp.AppwriteManager
import com.example.inventoryapp.databinding.ActivityAddEditItemBinding
import com.example.inventoryapp.models.Category
import com.example.inventoryapp.models.Warehouse
import com.example.inventoryapp.utils.UiUtils
import io.appwrite.ID
import io.appwrite.Query
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

class AddEditItemActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddEditItemBinding
    private var warehouses = listOf<Warehouse>()
    private var categories = listOf<Category>()
    private var selectedWarehouse: Warehouse? = null
    private var isEditMode = false
    private var itemId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditItemBinding.inflate(layoutInflater)
        setContentView(binding.root)

        itemId = intent.getStringExtra("itemId")
        isEditMode = itemId != null

        setupToolbar()
        loadWarehousesAndCategories()
        setupSaveButton()
        setupStatusDropdown()

        if (isEditMode) {
            loadItemData(itemId!!)
        } else {
            generateSku()
        }

        setupWarehouseChangeListener()
    }

    private suspend fun createActivityLog(
        itemId: String,
        actionType: String,
        oldValue: String? = null,
        newValue: String? = null,
        details: String? = null
    ) {
        try {
            val logData = mapOf(
                "userId" to "system",
                "itemId" to itemId,
                "actionType" to actionType,
                "oldValue" to (oldValue ?: ""),
                "newValue" to (newValue ?: ""),
                "details" to (details ?: "")
            )

            val logDoc = AppwriteManager.databases.createDocument(
                databaseId = AppwriteManager.DATABASE_ID,
                collectionId = AppwriteManager.Collections.ACTIVITY_LOGS,
                documentId = ID.unique(),
                data = logData
            )
        } catch (e: Exception) {
            Log.e("AddEditActivity", "Failed to create activity log: ${e.message}")
        }
    }

    private fun getItemDataMap(): Map<String, Any> {
        val warehouse = warehouses.find { it.name == binding.warehouseInput.text.toString() }
            ?: throw Exception("Invalid warehouse selected")
        val category = categories.find { it.name == binding.categoryInput.text.toString() }
            ?: throw Exception("Invalid category selected")

        return mapOf(
            "name" to binding.itemNameInput.text.toString().trim(),
            "warehouseId" to warehouse.id,
            "categoryId" to category.id,
            "price" to binding.priceInput.text.toString().toFloat(),
            "currentStock" to binding.currentStockInput.text.toString().toInt(),
            "minStock" to binding.minStockInput.text.toString().toInt(),
            "floor" to (binding.floorInput.text?.toString()?.trim() ?: ""),
            "section" to (binding.sectionInput.text?.toString()?.trim() ?: ""),
            "sku" to binding.skuInput.text.toString().trim(),
            "status" to binding.statusInput.text.toString().trim()
        )
    }

    private suspend fun validateSku(sku: String): Boolean {
        val queries = mutableListOf(Query.equal("sku", sku))
        if (isEditMode) {
            queries.add(Query.notEqual("\$id", itemId!!))
        }

        val existingItems = AppwriteManager.databases.listDocuments(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
            queries = queries
        )
        return existingItems.total == 0L
    }

    private fun validateFloorAndSection(warehouse: Warehouse, floor: String?, section: String?): Boolean {
        if (floor.isNullOrBlank() || section.isNullOrBlank()) return true
        return warehouse.floors.contains(floor) && warehouse.sections.contains(section)
    }

    private suspend fun isDuplicateLocation(
        name: String,
        warehouseId: String,
        floor: String,
        section: String
    ): Boolean {
        val queries = mutableListOf(
            Query.equal("name", name),
            Query.equal("warehouseId", warehouseId),
            Query.equal("floor", floor),
            Query.equal("section", section),
            Query.notEqual("status", "Discontinued")
        )

        if (isEditMode) {
            queries.add(Query.notEqual("\$id", itemId!!))
        }

        val existingItems = AppwriteManager.databases.listDocuments(
            databaseId = AppwriteManager.DATABASE_ID,
            collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
            queries = queries
        )

        return existingItems.total > 0
    }

    private fun showLoading(show: Boolean) {
        binding.saveButton.isEnabled = !show
        binding.saveButton.text = if (show) "Saving..." else "Save Item"
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = if (isEditMode) "Edit Item" else "Add New Item"
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun generateSku() {
        val uniqueSku = "SKU-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
        binding.skuInput.setText(uniqueSku)
    }

    private fun setupStatusDropdown() {
        val statusOptions = listOf("Active", "Inactive", "Discontinued")
        val statusAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            statusOptions
        )
        binding.statusInput.setAdapter(statusAdapter)

        if (!isEditMode) {
            binding.statusInput.setText("Active", false)
        }
    }

    private fun loadWarehousesAndCategories() {
        lifecycleScope.launch {
            try {
                val warehouseResponse = AppwriteManager.databases.listDocuments(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.WAREHOUSES
                )
                warehouses = warehouseResponse.documents.map {
                    Warehouse(
                        id = it.id,
                        name = it.data["name"] as String,
                        location = it.data["location"] as String,
                        address = it.data["address"] as? String,
                        floors = it.data["floors"] as List<String>,
                        sections = it.data["sections"] as List<String>
                    )
                }

                val categoryResponse = AppwriteManager.databases.listDocuments(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.CATEGORIES
                )
                categories = categoryResponse.documents.map {
                    Category(
                        id = it.id,
                        name = it.data["name"] as String,
                        description = it.data["description"] as? String
                    )
                }

                setupDropdowns()
            } catch (e: Exception) {
                UiUtils.showError(binding.root, "Failed to load data. Please check your connection.")
            }
        }
    }

    private fun setupDropdowns() {
        val warehouseAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            warehouses.map { it.name }
        )
        binding.warehouseInput.setAdapter(warehouseAdapter)

        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories.map { it.name }
        )
        binding.categoryInput.setAdapter(categoryAdapter)
    }

    private fun setupWarehouseChangeListener() {
        binding.warehouseInput.setOnItemClickListener { _, _, position, _ ->
            selectedWarehouse = warehouses[position]
            setupFloorAndSectionDropdowns()
        }
    }

    private fun setupFloorAndSectionDropdowns() {
        selectedWarehouse?.let { warehouse ->
            val floorAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                warehouse.floors
            )
            binding.floorInput.setAdapter(floorAdapter)

            val sectionAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                warehouse.sections
            )
            binding.sectionInput.setAdapter(sectionAdapter)
        }
    }

    private fun loadItemData(itemId: String) {
        lifecycleScope.launch {
            try {
                val item = AppwriteManager.databases.getDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                    documentId = itemId
                )

                binding.apply {
                    itemNameInput.setText(item.data["name"] as? String)
                    priceInput.setText((item.data["price"] as? Number)?.toString())
                    currentStockInput.setText((item.data["currentStock"] as? Number)?.toString())
                    minStockInput.setText((item.data["minStock"] as? Number)?.toString())
                    skuInput.setText(item.data["sku"] as? String)
                    statusInput.setText(item.data["status"] as? String, false)

                    val warehouse = warehouses.find { it.id == item.data["warehouseId"] }
                    warehouseInput.setText(warehouse?.name, false)
                    selectedWarehouse = warehouse

                    if (warehouse != null) {
                        setupFloorAndSectionDropdowns()
                        floorInput.setText(item.data["floor"] as? String, false)
                        sectionInput.setText(item.data["section"] as? String, false)
                    }

                    val category = categories.find { it.id == item.data["categoryId"] }
                    categoryInput.setText(category?.name, false)
                }
            } catch (e: Exception) {
                UiUtils.showError(binding.root, "Failed to load item data. Please try again.")
            }
        }
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            validateAndSave()
        }
    }

    private fun validateAndSave() {
        lifecycleScope.launch {
            try {
                if (!validateInputs()) return@launch

                showLoading(true)
                if (isEditMode) {
                    updateItem()
                } else {
                    addItem()
                }
            } catch (e: Exception) {
                showLoading(false)
                UiUtils.showError(binding.root, "Validation failed. Please check your inputs.")
            }
        }
    }

    private suspend fun validateInputs(): Boolean {
        var isValid = true
        binding.apply {
            // Reset errors
            itemNameLayout.error = null
            warehouseLayout.error = null
            categoryLayout.error = null
            priceLayout.error = null
            currentStockLayout.error = null
            minStockLayout.error = null
            statusLayout.error = null
            floorLayout.error = null
            sectionLayout.error = null

            // Required fields validation
            if (itemNameInput.text.isNullOrBlank()) {
                itemNameLayout.error = "Item name is required"
                isValid = false
            }

            if (warehouseInput.text.isNullOrBlank()) {
                warehouseLayout.error = "Warehouse is required"
                isValid = false
            }

            if (categoryInput.text.isNullOrBlank()) {
                categoryLayout.error = "Category is required"
                isValid = false
            }

            // Price validation
            val priceText = priceInput.text?.toString()
            if (priceText.isNullOrBlank() || priceText.toFloatOrNull() == null) {
                priceLayout.error = "Valid price is required"
                isValid = false
            } else if (priceText.toFloat() <= 0) {
                priceLayout.error = "Price must be greater than 0"
                isValid = false
            }

            // Stock validations
            val currentStockText = currentStockInput.text?.toString()
            val minStockText = minStockInput.text?.toString()

            if (currentStockText.isNullOrBlank() || currentStockText.toIntOrNull() == null) {
                currentStockLayout.error = "Valid current stock is required"
                isValid = false
            } else if (currentStockText.toInt() < 0) {
                currentStockLayout.error = "Current stock cannot be negative"
                isValid = false
            }

            if (minStockText.isNullOrBlank() || minStockText.toIntOrNull() == null) {
                minStockLayout.error = "Valid minimum stock is required"
                isValid = false
            } else if (minStockText.toInt() < 0) {
                minStockLayout.error = "Minimum stock cannot be negative"
                isValid = false
            }

            if (statusInput.text.isNullOrBlank()) {
                statusLayout.error = "Status is required"
                isValid = false
            }

            // Validate warehouse and floor/section
            val warehouse = warehouses.find { it.name == warehouseInput.text.toString() }
            if (warehouse == null) {
                warehouseLayout.error = "Invalid warehouse selected"
                isValid = false
            } else {
                val floor = floorInput.text?.toString()
                val section = sectionInput.text?.toString()
                if (!validateFloorAndSection(warehouse, floor, section)) {
                    floorLayout.error = "Invalid floor for selected warehouse"
                    sectionLayout.error = "Invalid section for selected warehouse"
                    isValid = false
                }

                // Check for duplicate location
                if (isValid && floor != null && section != null) {
                    if (isDuplicateLocation(
                            itemNameInput.text.toString(),
                            warehouse.id,
                            floor,
                            section
                        )
                    ) {
                        itemNameLayout.error = "An item with this name already exists in this location"
                        isValid = false
                    }
                }
            }

            // Validate SKU uniqueness
            if (isValid && !validateSku(skuInput.text.toString())) {
                skuLayout.error = "This SKU is already in use"
                isValid = false
            }
        }
        return isValid
    }

    private fun addItem() {
        lifecycleScope.launch {
            try {
                Log.d("AddEditActivity", "Starting to add item")
                val itemData = getItemDataMap()

                // Create the item
                Log.d("AddEditActivity", "Creating inventory item")
                val newItem = AppwriteManager.databases.createDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                    documentId = ID.unique(),
                    data = itemData
                )
                Log.d("AddEditActivity", "Created inventory item with ID: ${newItem.id}")

                // Create activity log
                Log.d("AddEditActivity", "Creating activity log")
                val itemDetails = mapOf(
                    "name" to itemData["name"],
                    "warehouse" to (warehouses.find { it.id == itemData["warehouseId"] }?.name ?: ""),
                    "category" to (categories.find { it.id == itemData["categoryId"] }?.name ?: ""),
                    "location" to "${itemData["floor"]}/${itemData["section"]}"
                )

                createActivityLog(
                    itemId = newItem.id,
                    actionType = "CREATE",
                    newValue = itemData.toString(),
                    details = "Created new item: ${itemDetails["name"]} in ${itemDetails["warehouse"]} at ${itemDetails["location"]}"
                )
                Log.d("AddEditActivity", "Created activity log")

                // Create initial stock movement record
                Log.d("AddEditActivity", "Creating stock movement")
                val stockMovementData = mapOf(
                    "itemId" to newItem.id,
                    "warehouseId" to itemData["warehouseId"],
                    "quantity" to itemData["currentStock"],
                    "type" to "INITIAL",
                    "reason" to "Initial stock entry",
                    "userId" to "system"
                )

                try {
                    AppwriteManager.databases.createDocument(
                        databaseId = AppwriteManager.DATABASE_ID,
                        collectionId = AppwriteManager.Collections.STOCK_MOVEMENTS,
                        documentId = ID.unique(),
                        data = stockMovementData
                    )
                    Log.d("AddEditActivity", "Created stock movement")
                } catch (e: Exception) {
                    Log.e("AddEditActivity", "Failed to create stock movement: ${e.message}", e)
                    // We'll continue even if stock movement creation fails
                }

                showLoading(false)
                Toast.makeText(this@AddEditItemActivity, "Item added successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Log.e("AddEditActivity", "Error in addItem: ${e.message}", e)
                showLoading(false)
                UiUtils.showError(binding.root, "Failed to add item: ${e.message}")
            }
        }
    }

    private fun updateItem() {
        lifecycleScope.launch {
            try {
                // Get current item data for comparison
                val currentItem = AppwriteManager.databases.getDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                    documentId = itemId!!
                )

                val newItemData = getItemDataMap()

                // Check for stock changes
                val oldStock = (currentItem.data["currentStock"] as? Number)?.toInt() ?: 0
                val newStock = newItemData["currentStock"] as Int

                // Update the item
                AppwriteManager.databases.updateDocument(
                    databaseId = AppwriteManager.DATABASE_ID,
                    collectionId = AppwriteManager.Collections.INVENTORY_ITEMS,
                    documentId = itemId!!,
                    data = newItemData
                )

                // Create activity log
                val itemDetails = mapOf(
                    "name" to newItemData["name"],
                    "warehouse" to (warehouses.find { it.id == newItemData["warehouseId"] }?.name ?: ""),
                    "category" to (categories.find { it.id == newItemData["categoryId"] }?.name ?: ""),
                    "location" to "${newItemData["floor"]}/${newItemData["section"]}"
                )

                createActivityLog(
                    itemId = itemId!!,
                    actionType = "UPDATE",
                    oldValue = currentItem.data.toString(),
                    newValue = newItemData.toString(),
                    details = "Updated item: ${itemDetails["name"]} in ${itemDetails["warehouse"]}"
                )

                // Create stock movement record if stock changed
                if (oldStock != newStock) {
                    val quantity = newStock - oldStock
                    val stockMovementData = mapOf(
                        "itemId" to itemId!!,
                        "warehouseId" to newItemData["warehouseId"],
                        "quantity" to quantity,
                        "type" to if (quantity > 0) "INCREASE" else "DECREASE",
                        "reason" to "Stock updated through edit",
                        "userId" to "system",
                        "timestamp" to Instant.now().toString()
                    )

                    AppwriteManager.databases.createDocument(
                        databaseId = AppwriteManager.DATABASE_ID,
                        collectionId = AppwriteManager.Collections.STOCK_MOVEMENTS,
                        documentId = ID.unique(),
                        data = stockMovementData
                    )
                }

                showLoading(false)
                Toast.makeText(this@AddEditItemActivity, "Item updated successfully", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                showLoading(false)
                val errorMessage = when {
                    e.message?.contains("network") == true -> "Network error. Please check your connection."
                    e.message?.contains("permission") == true -> "You don't have permission to perform this action."
                    else -> "Failed to update item. Please try again."
                }
                UiUtils.showError(binding.root, errorMessage)
            }
        }
    }
}
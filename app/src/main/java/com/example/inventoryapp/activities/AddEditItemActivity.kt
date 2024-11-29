package com.example.inventoryapp.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import com.example.inventoryapp.R
import com.example.inventoryapp.base.BaseActivity
import com.example.inventoryapp.databinding.ActivityAddEditItemBinding
import com.example.inventoryapp.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.UUID

class AddEditItemActivity : BaseActivity<ActivityAddEditItemBinding>() {
    private val firestore = FirebaseFirestore.getInstance()

    private var selectedWarehouse: Warehouse? = null
    private var selectedFloorId: String? = null
    private var selectedSectionId: String? = null

    // Predefined categories
    private val categories = listOf(
        "Electronics",
        "Office Supplies",
        "Furniture",
        "Raw Materials",
        "Finished Goods",
        "Packaging Materials"
    )

    override fun getLayoutResId(): Int = R.layout.activity_add_edit_item

    override fun inflateViewBinding(): ActivityAddEditItemBinding =
        ActivityAddEditItemBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupCategoryDropdown()
        loadWarehouseData()
    }

    override fun initializeViews() {
        binding.apply {
            // Set up toolbar
            toolbar.setNavigationOnClickListener { finish() }

            // Setup save button
            saveButton.setOnClickListener {
                if (validateInputs()) {
                    saveItem()
                }
            }

            // Clear errors on text change
            itemNameInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) itemNameLayout.error = null
            }
            priceInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) priceLayout.error = null
            }
            currentStockInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) currentStockLayout.error = null
            }
            minStockInput.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) minStockLayout.error = null
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categoryAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            categories
        )
        (binding.categoryInput as? AutoCompleteTextView)?.setAdapter(categoryAdapter)
    }

    private fun loadWarehouseData() {
        val warehouseId = intent.getStringExtra("warehouse_id")
        if (warehouseId == null) {
            showError("No warehouse selected")
            finish()
            return
        }

        showLoading(true)
        firestore.collection("warehouses")
            .document(warehouseId)
            .get()
            .addOnSuccessListener { doc ->
                val warehouse = doc.toObject(Warehouse::class.java)?.apply { id = doc.id }
                if (warehouse != null) {
                    selectedWarehouse = warehouse
                    setupLocationInputs(warehouse)
                    showLoading(false)
                } else {
                    showError("Warehouse not found")
                    finish()
                }
            }
            .addOnFailureListener {
                showError("Failed to load warehouse: ${it.message}")
                finish()
            }
    }

    private fun setupLocationInputs(warehouse: Warehouse) {
        binding.apply {
            // Set warehouse name
            warehouseInput.setText(warehouse.name)
            warehouseInput.isEnabled = false  // Since it's pre-selected

            // Setup floor dropdown
            val floors = warehouse.floors
            val floorItems = floors.map { it.value.name }
            val floorAdapter = ArrayAdapter(
                this@AddEditItemActivity,
                android.R.layout.simple_dropdown_item_1line,
                floorItems
            )
            floorInput.setAdapter(floorAdapter)

            // Floor selection listener
            floorInput.setOnItemClickListener { _, _, position, _ ->
                // Get selected floor ID
                selectedFloorId = floors.keys.toList()[position]
                // Reset section
                sectionInput.setText("")
                selectedSectionId = null

                // Update sections dropdown
                val sections = floors[selectedFloorId]?.sections ?: emptyMap()
                val sectionItems = sections.map { it.value.name }
                val sectionAdapter = ArrayAdapter(
                    this@AddEditItemActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    sectionItems
                )
                sectionInput.setAdapter(sectionAdapter)
            }

            // Section selection listener
            sectionInput.setOnItemClickListener { _, _, position, _ ->
                val sections = floors[selectedFloorId]?.sections ?: emptyMap()
                selectedSectionId = sections.keys.toList()[position]
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.apply {
            // Validate item name
            if (itemNameInput.text.isNullOrBlank()) {
                itemNameLayout.error = "Item name is required"
                isValid = false
            }

            // Validate category
            if (categoryInput.text.isNullOrBlank()) {
                categoryLayout.error = "Category is required"
                isValid = false
            }

            // Validate price
            val price = priceInput.text.toString().toDoubleOrNull()
            if (price == null || price <= 0) {
                priceLayout.error = "Enter a valid price"
                isValid = false
            }

            // Validate current stock
            val currentStock = currentStockInput.text.toString().toIntOrNull()
            if (currentStock == null || currentStock < 0) {
                currentStockLayout.error = "Enter a valid stock amount"
                isValid = false
            }

            // Validate minimum stock
            val minStock = minStockInput.text.toString().toIntOrNull()
            if (minStock == null || minStock < 0) {
                minStockLayout.error = "Enter a valid minimum stock"
                isValid = false
            }

            // Validate location selections
            if (selectedFloorId == null) {
                floorLayout.error = "Select a floor"
                isValid = false
            }
            if (selectedSectionId == null) {
                sectionLayout.error = "Select a section"
                isValid = false
            }
        }
        return isValid
    }

    private fun createInventoryItem(): InventoryItem {
        val warehouse = selectedWarehouse!!
        val floorName = warehouse.floors[selectedFloorId]?.name ?: ""
        val sectionName = warehouse.floors[selectedFloorId]?.sections?.get(selectedSectionId)?.name ?: ""

        return binding.run {
            InventoryItem(
                id = UUID.randomUUID().toString(),
                name = itemNameInput.text.toString().trim(),
                category = categoryInput.text.toString(),
                price = priceInput.text.toString().toDoubleOrNull() ?: 0.0,
                currentStock = currentStockInput.text.toString().toIntOrNull() ?: 0,
                minStock = minStockInput.text.toString().toIntOrNull() ?: 0,
                location = ItemLocation(
                    warehouseId = warehouse.id,
                    warehouseName = warehouse.name,
                    floor = floorName,
                    section = sectionName
                ),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    private fun saveItem() {
        val item = createInventoryItem()
        showLoading(true)

        // Create the category document if it doesn't exist, then add the item
        val categoryRef = firestore.collection("inventory")
            .document(item.category)

        firestore.runTransaction { transaction ->
            // First make sure category document exists
            transaction.set(categoryRef, mapOf("name" to item.category), SetOptions.merge())

            // Then add the item
            val itemRef = categoryRef.collection("items").document(item.id)
            transaction.set(itemRef, item)
        }.addOnSuccessListener {
            showSuccess("Item saved successfully")
            finish()
        }.addOnFailureListener { e ->
            showLoading(false)
            showError("Failed to save item: ${e.message}")
        }
    }
}
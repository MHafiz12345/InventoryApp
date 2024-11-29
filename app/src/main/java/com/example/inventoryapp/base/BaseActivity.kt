package com.example.inventoryapp.base

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.viewbinding.ViewBinding
import com.example.inventoryapp.utils.LoadingDialog
import com.example.inventoryapp.utils.Resource
import com.google.firebase.auth.FirebaseAuth

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    // ViewBinding instance, accessible in child classes
    private var _binding: VB? = null
    protected val binding get() = _binding!!

    // FirebaseAuth instance for authentication-related utilities
    protected val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // Loading dialog for showing progress to users
    private var loadingDialog: LoadingDialog? = null

    // Mandatory methods for subclasses
    @LayoutRes
    abstract fun getLayoutResId(): Int

    abstract fun inflateViewBinding(): VB

    // Lifecycle: Activity creation
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateViewBinding()
        setContentView(binding.root)

        // Initialize loading dialog
        loadingDialog = LoadingDialog(this)

        // Initialize views, listeners, and data observation
        initializeViews()
        setupListeners()
        observeData()
    }

    /**
     * Template method for initializing views.
     * Can be overridden by child activities if needed.
     */
    protected open fun initializeViews() {}

    /**
     * Template method for setting up listeners.
     * Can be overridden by child activities if needed.
     */
    protected open fun setupListeners() {}

    /**
     * Template method for observing data (LiveData, Flows, etc.).
     * Can be overridden by child activities if needed.
     */
    protected open fun observeData() {}

    /**
     * Shows or hides the loading dialog.
     * @param show `true` to show the loading dialog, `false` to hide it.
     */
    protected open fun showLoading(show: Boolean) {
        if (show) {
            // Check if loadingDialog exists and isn't showing already
            if (loadingDialog?.isShowing() == false) {
                loadingDialog?.show()
            }
        } else {
            // Dismiss if dialog is showing
            loadingDialog?.dismiss()
        }
    }

    /**
     * Displays an error message to the user via Toast.
     * @param message The error message to display.
     */
    protected open fun showError(message: String) {
        Toast.makeText(this, "Error: $message", Toast.LENGTH_LONG).show()
    }

    /**
     * Displays a success message to the user via Toast.
     * @param message The success message to display.
     */
    protected open fun showSuccess(message: String) {
        Toast.makeText(this, "Success: $message", Toast.LENGTH_SHORT).show()
    }

    /**
     * Retrieves the current logged-in Firebase user.
     * @return The current user, or `null` if no user is logged in.
     */
    protected fun getCurrentUser() = auth.currentUser

    /**
     * Checks if a user is currently logged in.
     * @return `true` if a user is logged in, `false` otherwise.
     */
    protected fun isUserLoggedIn() = auth.currentUser != null

    /**
     * Handles the state of a `Resource`.
     * @param resource The resource being handled.
     * @param onSuccess Callback for the success state.
     * @param onError Optional callback for the error state.
     */
    protected fun <T> handleResource(
        resource: Resource<T>,
        onSuccess: (T) -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        when (resource) {
            is Resource.Loading -> showLoading(true)
            is Resource.Success -> {
                showLoading(false)
                onSuccess(resource.data)
            }
            is Resource.Error -> {
                showLoading(false)
                onError?.invoke(resource.message) ?: showError(resource.message)
            }
        }
    }

    /**
     * Handles the back button in the toolbar if it exists.
     * @param item The selected menu item.
     * @return `true` if the event is handled, `false` otherwise.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    // Lifecycle: Activity destruction
    override fun onDestroy() {
        super.onDestroy()

        // Clean up resources
        loadingDialog?.cleanup()
        loadingDialog = null
        _binding = null
    }
}

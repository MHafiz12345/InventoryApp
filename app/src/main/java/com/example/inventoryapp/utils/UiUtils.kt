package com.example.inventoryapp.utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar

object UiUtils {
    fun showError(view: View, message: String, action: (() -> Unit)? = null) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        action?.let { actionFn ->
            snackbar.setAction("Retry") { actionFn() }
        }
        snackbar.show()
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
package com.example.inventoryapp.utils

import android.app.Dialog
import android.view.LayoutInflater
import androidx.fragment.app.FragmentActivity
import com.example.inventoryapp.R

class LoadingDialog(private val activity: FragmentActivity) {
    private var dialog: Dialog? = null

    fun show() {
        if (dialog == null) {
            dialog = Dialog(activity).apply {
                val view = LayoutInflater.from(activity).inflate(R.layout.loading_dialog, null)
                setContentView(view)
                setCancelable(false)
                window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
        dialog?.show()
    }

    fun dismiss() {
        dialog?.dismiss()
    }

    fun isShowing(): Boolean {
        return dialog?.isShowing == true
    }

    fun cleanup() {
        dialog?.dismiss()
        dialog = null
    }
}
package com.example.inventoryapp.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

fun Context.getFile(uri: Uri): File {
    val stream = contentResolver.openInputStream(uri)
    val file = File(cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")

    stream.use { input ->
        FileOutputStream(file).use { output ->
            input?.copyTo(output)
        }
    }

    return file
}
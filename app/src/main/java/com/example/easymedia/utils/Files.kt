package com.example.easymedia.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Convert Uri to a temp file in cache
 * Read from contentResolver and write to File
 */
object Files {
    fun uriToFile(context: Context, uri: Uri): File {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val fileName = "image_${System.currentTimeMillis()}.jpg"
        val file = File(context.cacheDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        return file
    }
}
package com.example.easymedia.ui.component.story.service

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.StoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import com.example.easymedia.ui.utils.IntentExtras
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class ImageRenderService(
    private val context: Context,
    private val storyRepository: StoryRepository
) {

    fun captureAndUpload(
        view: View,
        story: Story,
        isDownload: Boolean,
        onResult: () -> Unit
    ) {
        // Đảm bảo view đã render
        view.post {
            val bitmap = captureBitmapFromView(view)
            if (bitmap == null) {
                onResult()
                return@post
            }

            // Convert bitmap → file
            val imageFile = bitmapToFile(context, bitmap)

            // Nếu isDownload = true thì lưu vào thư viện
            if (isDownload) {
                val savedUri = saveImageToPublic(context, imageFile)
                if (savedUri != null) {
                    Log.d("ImageRenderService", "Saved image to gallery: $savedUri")
                } else {
                    Log.e("ImageRenderService", "Failed to save image")
                }
            }

            // Upload
            CoroutineScope(Dispatchers.IO).launch {
                val result = storyRepository.uploadStory(story, imageFile, false)
                if (result) {
                    // Gửi broadcast
                    val intent = Intent("com.example.easymedia.UPLOAD_DONE")
                    intent.putExtra(IntentExtras.RESULT_DATA_STR, true)
                    context.sendBroadcast(intent)
                }
            }
        }
    }

    private fun captureBitmapFromView(view: View): Bitmap? {
        if (view.width == 0 || view.height == 0) return null

        val bitmap = createBitmap(view.width, view.height)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return bitmap
    }

    private fun bitmapToFile(context: Context, bitmap: Bitmap): File {
        val file = File(context.cacheDir, "story_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it)
        }
        return file
    }

    private fun saveImageToPublic(context: Context, source: File): Uri? {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "story_${System.currentTimeMillis()}.jpg"
                )
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/EasyMedia")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values)!!

            resolver.openOutputStream(uri)?.use { out ->
                FileInputStream(source).use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

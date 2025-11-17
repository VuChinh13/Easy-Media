package com.example.easymedia.ui.component.story.service

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.example.easymedia.data.model.Story
import com.example.easymedia.data.repository.StoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap
import com.example.easymedia.ui.component.utils.IntentExtras
import java.io.File
import java.io.FileOutputStream

class ImageRenderService(
    private val context: Context,
    private val storyRepository: StoryRepository
) {

    fun captureAndUpload(
        view: View,
        story: Story,
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

            // Upload
            CoroutineScope(Dispatchers.IO).launch {
                val result = storyRepository.uploadStory(story, imageFile, false)

                // Gửi broadcast
                val intent = Intent("com.example.easymedia.UPLOAD_DONE")
                intent.putExtra(IntentExtras.RESULT_DATA_STR, true)
                context.sendBroadcast(intent)
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
}

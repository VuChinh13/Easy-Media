package com.example.instagram.data.data_source.cloudinary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

class CloudinaryServiceImpl(
    private val httpClient: OkHttpClient = OkHttpClient()
) : CloudinaryService {

    private val cloudName = "dsjsdyba7"
    private val unsignedPreset = "unsigned_preset"

    override suspend fun uploadImage(
        imageFile: File,
        folder: String
    ): CloudinaryUploadResult = withContext(Dispatchers.IO) {
        Log.d("CloudinaryService", "Bắt đầu upload ảnh: ${imageFile.name}, folder: $folder")

        val mediaType = "image/*".toMediaTypeOrNull()
        val fileBody = imageFile.asRequestBody(mediaType)

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", imageFile.name, fileBody)
            .addFormDataPart("upload_preset", unsignedPreset)
            .addFormDataPart("folder", folder)
            .build()

        val req = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
            .post(form)
            .build()

        httpClient.newCall(req).execute().use { resp ->
            Log.d("CloudinaryService", "HTTP status: ${resp.code}")

            if (!resp.isSuccessful) {
                Log.e("CloudinaryService", "Upload thất bại: ${resp.code} ${resp.message}")
                error("Cloudinary upload failed: ${resp.code} ${resp.message}")
            }

            val body = resp.body?.string() ?: error("Empty response body")
            val json = JSONObject(body)

            val secureUrl = json.getString("secure_url")
            val publicId = json.getString("public_id")

            Log.d("CloudinaryService", "Upload thành công!")
            Log.d("CloudinaryService", "URL: $secureUrl")
            Log.d("CloudinaryService", "Public ID: $publicId")

            CloudinaryUploadResult(
                secureUrl = secureUrl,
                publicId = publicId
            )
        }
    }
}

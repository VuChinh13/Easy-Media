package com.example.easymedia.data.data_source.cloudinary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class CloudinaryServiceImpl(
    private val httpClient: OkHttpClient = OkHttpClient()
) : CloudinaryService {

    private val cloudName = "dsjsdyba7"
    private val unsignedPreset = "unsigned_preset"
    private val apiKey = "471914241374597"
    private val apiSecret = "nCxIzNyM8p26vW1HvuchoiEZuM8"

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

    override suspend fun uploadVideo(
        videoFile: File,
        folder: String
    ): CloudinaryUploadResult = withContext(Dispatchers.IO) {
        Log.d("CloudinaryService", "Bắt đầu upload video: ${videoFile.name}, folder: $folder")

        val mediaType = "video/*".toMediaTypeOrNull()
        val fileBody = videoFile.asRequestBody(mediaType)

        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", videoFile.name, fileBody)
            .addFormDataPart("upload_preset", unsignedPreset)
            .addFormDataPart("folder", folder)
            .build()

        val req = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/video/upload")
            .post(form)
            .build()

        httpClient.newCall(req).execute().use { resp ->
            Log.d("CloudinaryService", "HTTP status: ${resp.code}")

            if (!resp.isSuccessful) {
                Log.e("CloudinaryService", "Upload thất bại: ${resp.code} ${resp.message}")
                error("Cloudinary video upload failed: ${resp.code} ${resp.message}")
            }

            val body = resp.body?.string() ?: error("Empty response body")
            val json = JSONObject(body)

            val secureUrl = json.getString("secure_url")
            val publicId = json.getString("public_id")

            Log.d("CloudinaryService", "Upload video thành công!")
            Log.d("CloudinaryService", "URL: $secureUrl")
            Log.d("CloudinaryService", "Public ID: $publicId")

            CloudinaryUploadResult(
                secureUrl = secureUrl,
                publicId = publicId
            )
        }
    }

    override suspend fun deleteImage(publicId: String): Boolean = withContext(Dispatchers.IO) {
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // Tạo signature: sha1("public_id=$publicId&timestamp=$timestamp$apiSecret")
        val toSign = "public_id=$publicId&timestamp=$timestamp$apiSecret"
        val signature = MessageDigest.getInstance("SHA-1")
            .digest(toSign.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val form = FormBody.Builder()
            .add("public_id", publicId)
            .add("api_key", apiKey)
            .add("timestamp", timestamp)
            .add("signature", signature)
            .build()

        val req = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/image/destroy")
            .post(form)
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Cloudinary delete failed: ${resp.code} ${resp.message}")

            val body = resp.body?.string() ?: return@use false
            val json = JSONObject(body)
            json.optString("result") == "ok"
        }
    }


    override suspend fun deleteVideo(publicId: String): Boolean = withContext(Dispatchers.IO) {
        val timestamp = (System.currentTimeMillis() / 1000).toString()

        // Tạo signature: sha1("public_id=$publicId&timestamp=$timestamp$apiSecret")
        val toSign = "public_id=$publicId&timestamp=$timestamp$apiSecret"
        val signature = MessageDigest.getInstance("SHA-1")
            .digest(toSign.toByteArray())
            .joinToString("") { "%02x".format(it) }

        val form = FormBody.Builder()
            .add("public_id", publicId)
            .add("api_key", apiKey)
            .add("timestamp", timestamp)
            .add("signature", signature)
            .build()

        val req = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$cloudName/video/destroy")
            .post(form)
            .build()

        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("Cloudinary delete video failed: ${resp.code} ${resp.message}")

            val body = resp.body?.string() ?: return@use false
            val json = JSONObject(body)
            // Cloudinary trả {"result":"ok"} khi xoá thành công
            json.optString("result") == "ok"
        }
    }
}

package com.example.easymedia.data.data_source.cloudinary

import java.io.File

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String
)

interface CloudinaryService {
    suspend fun uploadImage(
        imageFile: File,
        folder: String = "posts" // mặc định dùng folder 'posts'
    ): CloudinaryUploadResult

    suspend fun deleteImage(publicId: String): Boolean
    suspend fun uploadVideo(
        videoFile: File,
        folder: String
    ): CloudinaryUploadResult

    suspend fun deleteVideo(publicId: String): Boolean
}

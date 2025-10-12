package com.example.instagram.data.data_source.cloudinary

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
}

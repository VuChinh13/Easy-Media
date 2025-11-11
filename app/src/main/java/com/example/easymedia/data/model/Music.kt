package com.example.easymedia.data.model

import kotlin.time.Duration

data class Music(
    val title: String = "",       // Tên nhạc
    val artist: String = "",      // Tên nghệ sĩ
    val url: String = "",         // URL stream từ Cloudinary
    val publicId: String = "",    // Cloudinary Public ID
    val duration: String = ""
)

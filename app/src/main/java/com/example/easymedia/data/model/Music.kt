package com.example.easymedia.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Music(
    val title: String = "",       // Tên nhạc
    val artist: String = "",      // Tên nghệ sĩ
    val url: String = "",         // URL stream từ Cloudinary
    val publicId: String = "",    // Cloudinary Public ID
    val duration: String = "",
    val image: String = ""
) : Parcelable

package com.example.easymedia.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Location(
    val name: String = "",       // “Đà Lạt”
    val latitude: Double = 0.0,  // 11.9404
    val longitude: Double = 0.0, // 108.4583
    val address: String? = null  // “Phường 1, TP Đà Lạt, Lâm Đồng”
)
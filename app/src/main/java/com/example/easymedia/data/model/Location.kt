package com.example.easymedia.data.model

import android.os.Parcelable
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlinx.parcelize.Parcelize

@IgnoreExtraProperties
@Parcelize
data class Location(
    val latitude: Double = 0.0,  // 11.9404
    val longitude: Double = 0.0, // 108.4583
    val address: String? = null  // “Phường 1, TP Đà Lạt, Lâm Đồng”
): Parcelable
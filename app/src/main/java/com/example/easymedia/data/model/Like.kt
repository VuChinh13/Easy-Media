package com.example.easymedia.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Like(
    @get:PropertyName("liked_at") @set:PropertyName("liked_at")
    @ServerTimestamp
    var likedAt: Date? = null
)

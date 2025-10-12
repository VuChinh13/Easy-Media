package com.example.instagram.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class Comment(
    @DocumentId
    val id: String = "",

    @get:PropertyName("user_id") @set:PropertyName("user_id")
    var userId: String = "",

    val content: String = "",

    @get:PropertyName("created_at") @set:PropertyName("created_at")
    @ServerTimestamp
    var createdAt: Date? = null
)
